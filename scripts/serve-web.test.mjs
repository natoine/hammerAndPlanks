import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolvePath } from './serve-web.mjs';

const root = '/srv/web';

test('fichier du dossier servi', () => assert.equal(resolvePath(root, '/app.js'), '/srv/web/app.js'));
test('dossier : index.html', () => assert.equal(resolvePath(root, '/changelog/'), '/srv/web/changelog/index.html'));
test('paramètres d’URL ignorés', () => assert.equal(resolvePath(root, '/?control=force'), '/srv/web/index.html'));
test('caractères encodés décodés', () => assert.equal(resolvePath(root, '/a%20b.txt'), '/srv/web/a b.txt'));
test('remontée normalisée par l’URL : reste dans le dossier', () => {
  assert.equal(resolvePath(root, '/../etc/passwd'), '/srv/web/etc/passwd');
  assert.equal(resolvePath(root, '/%2e%2e/etc/passwd'), '/srv/web/etc/passwd');
});
test('remontée par barre oblique encodée refusée', () => assert.equal(resolvePath(root, '/..%2f..%2fetc/passwd'), null));
test('dossier voisin au nom proche refusé', () => assert.equal(resolvePath(root, '/..%2fweb2/x'), null));
