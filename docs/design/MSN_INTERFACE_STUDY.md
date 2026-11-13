# De MSN Messenger à une interface MSRN moderne

Copyright (C) 2026 GhostPunishR and contributors.
SPDX-License-Identifier: AGPL-3.0-only

Étude du 13 novembre 2026. Direction choisie par GhostPunishR : **MSN 7.5 et Windows Live Messenger 2009 modernisés**, pour une application Android Java/XML.

## Périmètre et méthode

Le terme MSN désigne ici la famille des clients de messagerie, pas le portail web MSN. L'étude compare les grandes générations Windows, leurs familles d'écrans et des variantes mobiles/web/Mac comme compléments. Elle ne prétend pas inventorier chaque build, bêta, localisation, thème tiers ou extension Messenger Plus. Windows Messenger livré avec XP est une branche voisine, pas un simple numéro interchangeable avec MSN Messenger.

Les annonces Microsoft servent à établir les fonctions et la chronologie. Les captures servent à observer la composition. Certaines galeries rétrospectives mélangent noms et dates : une image légendée comme une ancienne version montre déjà le message personnel et ne permet donc pas de dater l'introduction de cette fonction. Les captures officielles de 2008 examinées correspondent à la bêta préparant 2009. Les anciens liens d'images du communiqué de 2003 renvoient aujourd'hui une erreur 404. Ces limites sont conservées plutôt que comblées par des affirmations.

Les images historiques ont été consultées hors du dépôt. La maquette et les ressources Android de MSRN sont originales ; aucun logo, personnage, fond, son ou capture Microsoft n'est livré dans l'application.

## Les grandes générations

| Famille | Composition et usage observés | Enseignement pour MSRN |
| --- | --- | --- |
| Messenger Service 1 à 3, 1999-2000 ; famille Windows Messenger XP | Fenêtre étroite, liste et disponibilité au centre, menus/boutons compacts. Une capture avec boîte « À propos » confirme le build 2.2 ; la version exacte de certaines autres captures anciennes reste incertaine. | Priorité aux personnes, à la disponibilité et à une action de conversation immédiatement compréhensible. |
| MSN Messenger 5-6, 2002-2003 | Passage vers une identité visuelle plus personnelle. Le communiqué de MSN 6 confirme avatars, fonds et émoticônes personnalisables, webcam et jeux. | Donner une place identifiable à l'avatar et à l'expression personnelle, dans une structure lisible. |
| MSN Messenger 6.2, 7 et 7.5, 2004-2005 | Dans les captures 7.5, identité en haut, groupes de contacts, conversation séparée, photos encadrées et barre d'expression près de la saisie. Le communiqué 7.0 confirme message personnel, Winks et partage plus expressif. | Conserver pseudo, message personnel, présence et Wizz. Placer les moyens d'expression près du clavier, en laissant la lecture respirer. |
| Windows Live Messenger 8 et 8.5, 2006-2007 | En-tête plus coloré, avatar encadré, recherche et groupes. Le relief et les panneaux prennent une place importante. | Reprendre la sensation accueillante du bleu, avec des surfaces plus calmes et des commandes tactiles espacées. |
| Windows Live Messenger 2009, vague préparée en 2008 | Le blog Microsoft documente une palette plus légère, favoris, scènes personnalisées, actions de contact et couleurs de présence cohérentes. Les captures montrent recherche, identité et liste clairement structurées. | En-tête évoquant une scène, avatars à cadre de présence, recherche et regroupement des contacts. Les favoris et scènes personnalisables restent des évolutions, pas des contrôles fictifs. |
| Windows Live Messenger 2011-2012, famille tardive | Les captures 2011 et revues de l'époque montrent une place accrue au social et une alternative compacte centrée sur les contacts. La couverture visuelle directe porte ici sur 2011, pas chaque build 2012. | Garder l'accès rapide aux conversations. Un éventuel fil social doit rester un choix distinct dans une phase ultérieure. |

Références : [lancement Microsoft en 1999][s1], [MSN 6 en 2003][s2], [MSN 7 en 2005][s3], [présentation officielle de la vague 2009][s4], [capture et galerie MSN 7.5][s5], [galerie rétrospective][s6], [revue de Windows Live Messenger 2011][s7]. Les interprétations de la troisième colonne sont nos décisions de conception, pas des déclarations Microsoft.

## Variantes et autres familles d'écrans

Les clients de bureau ne résument pas toute la famille. Les captures iPhone de 2010 montrent déjà des listes tactiles, des avatars encadrés et une navigation basse : c'est une référence utile pour le passage à Android. Le Web Messenger rendait les contacts et le chat accessibles depuis un navigateur. Messenger pour Mac suivait son propre calendrier et son intégration au système ; les notes officielles de sa bêta 8 documentent notamment l'arrivée audio/vidéo. Leur couverture est complémentaire, sans supposer une parité exacte avec Windows.

Sources : [captures iPhone, Web Design Museum][s8], [annonce Microsoft incluant Web Messenger][s9], [notes Microsoft de Messenger Mac 8 bêta][s10].

| Famille d'écran ou interaction historique | Adaptation moderne proposée | État dans cette livraison |
| --- | --- | --- |
| Connexion, identité, inscription | Accueil sobre, labels explicites, mot de passe masqué, action principale identifiable. | Écran Compte restylé ; authentification actuelle conservée. |
| Liste principale, groupes et présence | Cartes de contact avec avatar, pseudo, identifiant, état écrit, message personnel et badge non lu. Recherche locale tolérante aux accents. | Implémenté, groupes Connectés/Hors ligne et recherche sur les contacts déjà chargés. |
| Conversation individuelle et historique | Identité compacte en haut, bulles alignées, dates, anciens messages dans la zone défilante, saisie proche des actions. | Implémenté, flux HTTP/WebSocket actuel conservé. |
| Ajouter/accepter/refuser un contact | Formulaire court et invitations en cartes avec deux décisions lisibles. | Implémenté, API existante conservée. |
| Avatar, pseudo, humeur et présence | Profil organisé en sections : identité, message personnel, disponibilité, sons et compte. | Implémenté avec les avatars prédéfinis existants. |
| Emoticônes, Wizz, sons et retours de réception | Expression à proximité de la saisie, son optionnel, limitation des sollicitations. | Fonctions actuelles conservées ; anciens tests de sons maintenus. |
| Fichiers, partage de photos, messages vocaux | Pièces jointes identifiées, progression, erreur/reprise, contrôle avant ouverture. | Conception future ; aucun bouton actif de pièce jointe ajouté. |
| Appels audio, webcam, conversation de groupe | Surfaces dédiées, participants et permissions explicites, contrôles adaptés au tactile. | Conception future, hors capacités actuelles du serveur. |
| Cartes de contact, favoris, catégories et scènes personnelles | Fiche cohérente et personnalisation sobre, favoris séparés de la présence. | Principes retenus ; persistance et commandes à développer séparément. |
| Réglages, confidentialité, blocage et gestion du compte | Paramètres accessibles, choix compréhensibles et confirmations pour actions sensibles. | Sons et compte existants présentés clairement ; blocage/suppression restent au programme. |
| Alertes de connexion, déconnexion, saisie, échec et vide | États explicites, lisibles sans couleur seule, absence de fausse promesse de livraison. | États existants conservés ; indicateur de saisie masqué quand inactif. |

## Direction visuelle retenue

**Les proches, simplement.** L'identité de MSRN doit évoquer la proximité et la personnalité de 7.5/2009 avec une composition adaptée à un téléphone actuel.

- Le bleu sert aux actions et à une scène d'en-tête discrète. Les grands aplats décoratifs et le relief des anciennes fenêtres deviennent des surfaces et des espacements.
- L'avatar reste un repère fort. Son cadre traduit la présence, toujours doublée d'un libellé. Invisible et états inconnus restent hors ligne dans la liste.
- La liste principale reste une liste de personnes. Les rubriques, le pseudo et le message personnel comptent davantage qu'un flux social.
- Les conversations privilégient le contenu : bulles, date locale et saisie. Wizz et emoji restent visibles ; les actions d'actualisation et de pagination occupent moins de place.
- La navigation basse conserve Contacts, Chat, Amis et Profil. L'onglet courant est indiqué. La marque globale s'efface dans le chat pour laisser plus de hauteur à la conversation.
- Deux palettes sont prévues dès maintenant, claire et sombre selon le système. Aucune police ou ressource distante n'est nécessaire.

## Système de conception implémenté

| Rôle | Clair | Sombre |
| --- | --- | --- |
| Fond | `#F4F8FC` | `#101A2A` |
| Surface | `#FFFFFF` | `#19283D` |
| Surface secondaire | `#EAF1FB` | `#233851` |
| Texte principal | `#142C49` | `#EAF2FF` |
| Texte secondaire | `#52647B` | `#B4C5DA` |
| Action principale | `#245DCC` | `#A6C4FF` |
| Texte sur action | `#FFFFFF` | `#10213E` |

Les couleurs proviennent des fichiers `values/colors.xml` et `values-night/colors.xml`. Les composants Android utilisent ces rôles, y compris les bulles et les cartes générées en Java. Les contrôles principaux ont une hauteur minimale de 48 dp, les polices utilisent des sp et les formulaires longs défilent. Les icônes de rafraîchissement et d'envoi gardent une description accessible. La ligne de contact est une seule action accessible avec une description complète, y compris le nombre réel de messages non lus lorsque le badge visuel est plafonné à 99+.

La recherche est locale et s'applique aux contacts déjà retournés par l'API, pas à un annuaire global. Elle cherche dans pseudo, identifiant et message personnel, sans modifier les données envoyées au serveur. Les états de présence et l'appartenance aux contacts restent décidés par le serveur.

Référence d'accessibilité : [Android Developers, accessibilité des Views][s11]. Les valeurs graphiques et la composition sont des choix originaux MSRN.

## Livrables et vérification

- `msrn-modern-preview.html` : proposition interactive autonome, cinq écrans, thèmes clair/sombre, données fictives. Les interactions sont simulées localement, sans serveur ni création de compte.
- `msrn-modern-overview.png` et `msrn-modern-overview-dark.png` : aperçus des trois écrans principaux, rendus depuis cette maquette. Ce ne sont pas des captures Android.
- Application : six layouts XML existants (structure principale et cinq écrans), thème, composants natifs, cartes dynamiques et recherche locale modernisés. Aucun contrat HTTP, SQL ou WebSocket modifié.
- Compilation : `gradle -p android --no-daemon --max-workers=2 :app:assembleDebug :app:testDebugUnitTest` a produit l'APK et exécuté les 18 tests avec succès, dont 6 nouveaux tests de recherche, Unicode, présence et compteurs.
- Revue indépendante : les 50 identifiants d'interface existants conservent leur type de widget ; aucun défaut bloquant identifié dans les changements natifs examinés.

La validation reste partielle sur appareil : clavier Android, grandes polices, orientation, TalkBack, rendu des menus et couleurs système doivent être essayés sur un émulateur/appareil. La maquette navigateur et les tests JVM ne prouvent pas ces comportements. Les essais audio de la livraison précédente restent eux aussi à réaliser sur Android.

## Sources consultées

1. [Microsoft, lancement de Messenger Service, juillet 1999][s1] : source primaire pour les débuts et la présence.
2. [Microsoft, présentation de MSN 6, juin 2003][s2] : personnalisation et fonctions annoncées.
3. [Microsoft, disponibilité de MSN 7, avril 2005][s3] : expression, message personnel et partage.
4. [Windows Experience Blog, septembre 2008][s4] : source primaire et captures de la bêta préparant 2009.
5. [LO4D, galerie MSN 7.5][s5] : référence visuelle contacts et conversation.
6. [Marcelo Pedra, rétrospective illustrée][s6] : complément visuel, avec les réserves de légendage précisées plus haut.
7. [TechRadar, revue du Messenger de la génération 2011][s7] : vues sociale et compacte.
8. [Web Design Museum, Messenger iPhone 2010][s8] : captures de la déclinaison tactile.
9. [Microsoft, annonces de décembre 2004][s9] : Web Messenger et interactions expressives.
10. [Microsoft, notes de Messenger Mac 8 bêta, mars 2010][s10] : document primaire conservé sur un hébergeur tiers.
11. [Android Developers, accessibilité des Views][s11] : cibles tactiles, descriptions et lisibilité.

[s1]: https://news.microsoft.com/source/1999/07/21/microsoft-launches-msn-messenger-service/
[s2]: https://news.microsoft.com/source/2003/06/18/msn-messenger-6-allows-im-lovers-to-express-themselves-with-style/
[s3]: https://news.microsoft.com/source/2005/04/07/global-availability-of-msn-messenger-and-msn-spaces-connects-people-around-the-world/
[s4]: https://blogs.windows.com/windowsexperience/2008/09/17/next-wave-of-windows-live-introduced-with-new-betas/
[s5]: https://msn-messenger-7-5.en.lo4d.com/screenshots
[s6]: https://www.marcelopedra.com.ar/blog/2013/05/08/historia-y-evolucion-de-la-interfaz-grafica-de-msn-messenger/
[s7]: https://www.techradar.com/reviews/pc-mac/software/home-and-reference-software/microsoft-windows-live-messenger-705276/review
[s8]: https://www.webdesignmuseum.org/iphone/windows-live-messenger-for-iphone-in-2010
[s9]: https://news.microsoft.com/source/2004/12/01/msn-introduces-new-communication-service-that-enables-blogging-picture-sharing-and-more/
[s10]: https://9to5mac.com/wp-content/uploads/sites/6/2010/03/messenger8beta_releasenotes.pdf
[s11]: https://developer.android.com/guide/topics/ui/accessibility/views/apps-views
