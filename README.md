# Citizen Mancie (citizenmancie)

Mod NeoForge **21.1.215** / Minecraft **1.21.1** / Java 21 pour le serveur RP Citizen.

## Joueurs
- **Maintenir G** : la roulette s'ouvre, vise un sort à la souris, **relâche G** pour le lancer.
- Appui bref sur G : la roulette reste ouverte → clic gauche (ou G) pour lancer, clic droit / Échap pour fermer.
- `/sorts` : voir son grimoire.
- `/sort <id>` : lancer un sort sans la roulette (joueurs console / Bedrock).
- On ne peut **jamais** apprendre un sort seul : seul le staff l'enseigne.

## Staff (OP niveau 2)
| Commande | Effet |
|---|---|
| `/mancie donner <joueur> <sort>` | Enseigne un sort (sauvegardé, gardé à la mort) |
| `/mancie ecole <joueur> <mancie>` | Enseigne tous les sorts d'une mancie |
| `/mancie retirer <joueur> <sort>` | Retire un sort |
| `/mancie vider <joueur>` | Retire tous les sorts |
| `/mancie voir <joueur>` | Voir le grimoire d'un joueur |
| `/mancie liste` | Liste de tous les sorts et leurs id |

## Sorts
| Mancie | id | Effet | Recharge |
|---|---|---|---|
| ✹ Pyromancie | `trait_de_feu` | Jet de flammes 20 blocs, 5 dégâts + brûlure 5 s | 3 s |
| | `embrasement` | Enflamme les créatures à 5 blocs | 15 s |
| | `peau_de_braise` | Résistance au feu 45 s | 45 s |
| ☀ Luminomancie | `lumiere_guerisseuse` | Soin 3 cœurs (2 aux joueurs proches) | 20 s |
| | `eblouissement` | Aveuglement 5 s à 6 blocs | 20 s |
| | `revelation` | Fait briller les créatures à 24 blocs + vision nocturne | 30 s |
| ❤ Hématomancie | `drain_de_sang` | Vole 2 cœurs à la cible (12 blocs) | 8 s |
| | `lame_de_sang` | Coûte 1,5 cœur : 8 dégâts (16 blocs) | 5 s |
| | `pacte_de_sang` | Coûte 3 cœurs : Force II + Vitesse 20 s | 60 s |

Aucun sort ne met le feu aux blocs (sécurité pour les villes en bois).
Les sorts blessent aussi les joueurs, même si le PvP est désactivé sur le serveur.
Mancies valides pour `/mancie ecole` : `pyromancie`, `luminomancie`, `hematomancie`.

## Compilation
Pousser sur la branche `main` du dépôt GitHub : GitHub Actions compile et publie le `.jar` dans **Releases**.
En local : `./gradlew build` → `build/libs/citizenmancie-1.0.2.jar`.

À installer sur le **serveur** et chez les **joueurs PC** (roulette). Les joueurs sans le mod peuvent quand même se connecter et utiliser `/sort`.
