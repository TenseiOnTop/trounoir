#!/bin/bash
# Test de démarrage : lance un serveur dédié avec le mod, exécute quelques commandes, l'arrête,
# puis échoue si le journal contient une erreur du mod ou une exception.
set -u
mkdir -p run
echo "eula=true" > run/eula.txt
printf "online-mode=false\nlevel-seed=trounoir\n" > run/server.properties
rm -f /tmp/srv_in; mkfifo /tmp/srv_in
( ./gradlew runServer --console=plain < /tmp/srv_in > /tmp/server.log 2>&1; echo "EXIT $?" >> /tmp/server.log ) &
exec 3>/tmp/srv_in
for i in $(seq 1 180); do
  grep -q "Done (" /tmp/server.log && break
  grep -q "^EXIT " /tmp/server.log && break
  sleep 5
done
if grep -q "Done (" /tmp/server.log; then
  echo "trounoir site list" >&3; sleep 5
  echo "reload" >&3; sleep 15
  echo "execute in trounoir:diviniarth run say diviniarth-ok" >&3; sleep 10
  echo "stop" >&3
fi
for i in $(seq 1 60); do grep -q "^EXIT " /tmp/server.log && break; sleep 5; done
exec 3>&-
echo "===== extrait du journal ====="
grep -E "Trou-Noir|trounoir|ERROR|Exception|Done \(|diviniarth-ok|EXIT" /tmp/server.log | grep -v "^\s*at " | head -200
fail=0
grep -q "Done (" /tmp/server.log || { echo "ECHEC : le serveur n'a pas démarré"; fail=1; }
grep -q "diviniarth-ok" /tmp/server.log || { echo "ECHEC : la dimension Diviniarth n'est pas chargée"; fail=1; }
grep -E "\[Trou-Noir\].*(ignoré|Échec|illisible|inconnu|Impossible)" /tmp/server.log && { echo "ECHEC : erreurs du mod"; fail=1; }
grep -nE "Exception|Caused by" /tmp/server.log | grep -iE "trounoir" && { echo "ECHEC : exception liée au mod"; fail=1; }
exit $fail
