// Estimation de pose InstantHMR dans le navigateur (onnxruntime-web, WebAssembly), pour le mode caméra.
// Même chaîne que les modules Kotlin :pose (PosePreprocessor, RoiTracker) : carré 1,2x autour de la personne,
// 224x224, normalisation ImageNet, CLIFF en pixels normalisés, suivi de la zone par les points du haut du corps.
// Différence : le redimensionnement est fait par le navigateur (drawImage), et non par interpolation bilinéaire maison.
// Le modèle et ONNX Runtime ne sont chargés qu'au premier passage en mode caméra. Aucune image ne quitte le navigateur.
(function () {
  "use strict";
  if (window.hapPose) return; // déjà fourni (tests d'interface)

  var SIZE = 224, EXPAND = 1.2, PERIOD_MS = 50; // 20 Hz au plus
  var MEAN = [0.485, 0.456, 0.406], STD = [0.229, 0.224, 0.225];
  var UPPER_BODY = [0, 1, 2, 3, 4, 5, 6, 7, 8, 67, 68, 69]; // Mhr70.UPPER_BODY
  var MARGIN = 0.35, SMOOTHING = 0.5, MIN_SIDE = 0.15;       // RoiTracker
  var MODEL_URL = "models/instanthmr.onnx";

  var status = "OFF", running = false, session = null, loading = null, latest = null, box = null, timer = null;
  var canvas = document.createElement("canvas");
  canvas.width = canvas.height = SIZE;
  var ctx = canvas.getContext("2d", { willReadFrequently: true });
  var input = new Float32Array(3 * SIZE * SIZE);

  function loadScript(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = src; s.onload = resolve; s.onerror = function () { reject(new Error("Script introuvable : " + src)); };
      document.head.appendChild(s);
    });
  }

  function ensureSession() {
    if (!loading) {
      loading = (async function () {
        var head = await fetch(MODEL_URL, { method: "HEAD" });
        if (!head.ok) { var e = new Error("Modèle absent"); e.noModel = true; throw e; }
        await loadScript("ort/ort.wasm.min.js");
        ort.env.wasm.wasmPaths = new URL("ort/", window.location.href).href; // URL absolue exigée
        // Plusieurs threads seulement si la page est isolée (en-têtes COOP/COEP), sinon un seul.
        ort.env.wasm.numThreads = self.crossOriginIsolated ? Math.min(4, navigator.hardwareConcurrency || 1) : 1;
        session = await ort.InferenceSession.create(MODEL_URL, { executionProviders: ["wasm"], graphOptimizationLevel: "all" });
        return session;
      })();
      loading.catch(function () { loading = null; });
    }
    return loading;
  }

  function track(px, w, h) {
    var x1 = Infinity, y1 = Infinity, x2 = -Infinity, y2 = -Infinity;
    for (var k = 0; k < UPPER_BODY.length; k++) {
      var j = UPPER_BODY[k], x = px[2 * j], y = px[2 * j + 1];
      if (!isFinite(x) || !isFinite(y)) { box = null; return; }
      x1 = Math.min(x1, x); y1 = Math.min(y1, y); x2 = Math.max(x2, x); y2 = Math.max(y2, y);
    }
    var side = Math.max(x2 - x1, y2 - y1), cx = (x1 + x2) / 2, cy = (y1 + y2) / 2;
    if (side < MIN_SIDE * Math.min(w, h) || cx < 0 || cx > w || cy < 0 || cy > h) { box = null; return; }
    var m = side * MARGIN, next = [x1 - m, y1 - m, x2 + m, y2 + 2 * m];
    box = box ? box.map(function (v, i) { return v + (next[i] - v) * SMOOTHING; }) : next;
  }

  async function step() {
    if (!running) return;
    var started = performance.now();
    var video = document.getElementById("camera-thumbnail");
    try {
      if (video && video.readyState >= 2 && video.videoWidth > 0) {
        var W = video.videoWidth, H = video.videoHeight, b = box || [0, 0, W, H];
        var cx = (b[0] + b[2]) / 2, cy = (b[1] + b[3]) / 2, side = Math.max(b[2] - b[0], b[3] - b[1]);
        var size = side * EXPAND, x1 = cx - size / 2, y1 = cy - size / 2, k = SIZE / size;
        ctx.fillStyle = "#000"; ctx.fillRect(0, 0, SIZE, SIZE); // hors image : noir
        var sx = Math.max(0, x1), sy = Math.max(0, y1), ex = Math.min(W, x1 + size), ey = Math.min(H, y1 + size);
        if (ex > sx && ey > sy) ctx.drawImage(video, sx, sy, ex - sx, ey - sy, (sx - x1) * k, (sy - y1) * k, (ex - sx) * k, (ey - sy) * k);
        var data = ctx.getImageData(0, 0, SIZE, SIZE).data, plane = SIZE * SIZE;
        for (var i = 0; i < plane; i++) for (var c = 0; c < 3; c++) input[c * plane + i] = (data[i * 4 + c] / 255 - MEAN[c]) / STD[c];
        var feeds = {};
        feeds[session.inputNames[0]] = new ort.Tensor("float32", input, [1, 3, SIZE, SIZE]);
        feeds[session.inputNames[1]] = new ort.Tensor("float32", new Float32Array([2 * cx / W - 1, 2 * cy / H - 1, side / Math.max(W, H)]), [1, 3]);
        var timeS = started / 1000;
        var out = await session.run(feeds);
        if (!running) return;
        var j2 = out.joints_2d.data, j3 = out.joints_3d.data, px = new Float32Array(140), scale = size / SIZE;
        for (var n = 0; n < 70; n++) {
          px[2 * n] = (j2[2 * n] + 1) * 0.5 * SIZE * scale + x1;
          px[2 * n + 1] = (j2[2 * n + 1] + 1) * 0.5 * SIZE * scale + y1;
        }
        track(px, W, H);
        latest = { t: timeS, joints3d: Float32Array.from(j3), joints2d: px, width: W, height: H, ms: performance.now() - started };
        status = "RUNNING";
      }
    } catch (e) {
      console.error("Pose : inférence impossible", e);
      status = "ERROR"; running = false; return;
    }
    timer = setTimeout(step, Math.max(0, PERIOD_MS - (performance.now() - started)));
  }

  window.hapPose = {
    status: function () { return status; },
    now: function () { return performance.now() / 1000; },
    start: function () {
      if (running) return;
      running = true; status = "LOADING";
      ensureSession().then(function () { if (running) step(); }).catch(function (e) {
        running = false;
        status = e && e.noModel ? "NO_MODEL" : "ERROR";
        console.error("Pose : chargement impossible", e);
      });
    },
    stop: function () {
      running = false; clearTimeout(timer); latest = null; box = null;
      if (status === "RUNNING" || status === "LOADING") status = "OFF";
    },
    // Dernière pose : horodatage (s, -1 si aucune), points 3D (70x3) et 2D (70x2), taille de l'image.
    time: function () { return latest ? latest.t : -1; },
    joint3d: function (i) { return latest ? latest.joints3d[i] : NaN; },
    joint2d: function (i) { return latest ? latest.joints2d[i] : NaN; },
    width: function () { return latest ? latest.width : 0; },
    height: function () { return latest ? latest.height : 0; },
    inferenceMs: function () { return latest ? latest.ms : -1; }
  };
})();
