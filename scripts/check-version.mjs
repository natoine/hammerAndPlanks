// Vérifie que la version de package.json correspond à la dernière release du CHANGELOG.
import { readFileSync } from 'node:fs';

const version = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8')).version;
const changelog = readFileSync(new URL('../CHANGELOG.md', import.meta.url), 'utf8');
const latest = changelog.match(/^## \[(\d+\.\d+\.\d+)\]/m)?.[1];

if (latest !== version) {
  console.error(`Version incohérente : package.json=${version}, dernière release du CHANGELOG=${latest ?? 'aucune'}`);
  process.exit(1);
}
console.log(`Version cohérente : ${version}`);
