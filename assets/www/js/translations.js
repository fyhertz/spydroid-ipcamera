(function () {

    var translations = {
	en: {
	    1:"About",
	    2:"Return",
	    3:"Change quality settings",
	    4:"Toggle flash",
	    5:"Click on the torch to enable or disable the flash",
	    6:"Play a prerecorded sound",
	    7:"Connect !!",
	    8:"Disconnect ?!",
	    9:"STATUS",
	    10:"NOT CONNECTED",
	    11:"ERROR",
	    12:"CONNECTION",
	    13:"UPDATING SETTINGS",
	    14:"CONNECTED",
	    15:"Show some tips",
	    16:"Hide those tips",
	    17:"Those buttons will trigger sounds on your phone...",
	    18:"Use them to surprise your victim.",
	    19:"Or you could use this to surprise your victim !",
	    20:"This will simply toggle the led in front of you're phone, so that even in the deepest darkness, you will not be blind...",
	    21:"If the stream is choppy, try reducing the bitrate or increasing the cache size.",
	    22:"Try it instead of H.263 if video streaming is not working at all !",
	    23:"The H.264 compression algorithm is more efficient but may not work on your phone...",
	    24:"You need to install VLC first !",
	    25:"During the installation make sure to check the firefox plugin !",
	    26:"Close",
	    27:"You must leave the screen of your smartphone on !"
	},
	fr: {
	    1:"À propos",
	    2:"Retour",
	    3:"Changer la qualité du stream",
	    4:"Allumer/Éteindre le flash",
	    5:"Clique sur l'ampoule pour activer ou désactiver le flash",
	    6:"Jouer un son préenregistré",
	    7:"Connexion !!",
	    8:"Déconnecter ?!",
	    9:"STATUT",
	    10:"DÉCONNECTÉ",
	    11:"ERREUR",
	    12:"CONNEXION",
	    13:"M.A.J.",
	    14:"CONNECTÉ",
	    15:"Afficher l'aide",
	    16:"Masquer l'aide",
	    17:"Clique sur un de ces boutons pour lancer un son préenregistré sur ton smartphone !",
	    18:"Utilise les pour surprendre ta victime !!",
	    19:"Ça peut aussi servir à surprendre ta victime !",
	    20:"Clique sur l'ampoule pour allumer le flash de ton smartphone",
	    21:"Si le stream est saccadé essaye de réduire le bitrate ou d'augmenter la taille du cache.",
	    22:"Essaye le à la place du H.263 si le streaming de la vidéo ne marche pas du tout !",
	    23:"Le H.264 est un algo plus  efficace pour compresser la vidéo mais il a moins de chance de marcher sur ton smartphone...",
	    24:"Tu dois d'abord installer VLC !!",
	    25:"Pendant l'installation laisse cochée l'option plugin mozilla !",
	    26:"Fermer",
	    27:"Tu dois laisser l'écran de ton smartphone allumé"
	},
	de : {
	    1:"Apropos",
	    2:"Zurück",
	    3:"Qualität des Streams verändern",
	    4:"Fotolicht ein/aus",
	    5:"Klick die Glühbirne an, um das Fotolicht einzuschalten oder abzufallen",
	    6:"Vereinbarten Ton spielen",
	    7:"Verbindung !!",
	    8:"Verbinden ?!",
	    9:"STATUS",
	    10:"NICHT VERBUNDEN",
	    11:"FEHLER",
	    12:"VERBINDUNG",
	    13:"UPDATE",
	    14:"VERBUNDEN",
	    15:"Hilfe anzeigen",
	    16:"Hilfe ausblenden",
	    17:"Klick diese Tasten an, um Töne auf deinem Smartphone spielen zu lassen !",
	    18:"Benutz sie, um deine Opfer zu überraschen !!",
	    19:"Das kann auch dein Opfer erschrecken !",
	    20:"Es wird die LED hinter deinem Handy anmachen, damit du nie blind bleibst, auch im tiefsten Dunkeln",
	    21:"Wenn das Stream ruckartig ist, versuch mal die Bitrate zu reduzieren oder die Größe vom Cache zu steigern.",
	    22:"Probier es ansatt H.263, wenn das Videostream überhaupt nicht funktionert !",
	    23:"Der H.264 Kompressionalgorithmus ist effizienter aber er wird auf deinem Handy vielleicht nicht funktionieren...",
	    24:"Du musst zuerst VLC installieren !!",
	    25:"Während der Installation, prüfe dass das Firefox plugin abgecheckt ist!",
	    26:"Zumachen",
	    27:"Du musst den Bildschirm deines Smartphones eingeschaltet lassen !"    
	}
    };

    var lang = window.navigator.userLanguage || window.navigator.language;
    //var lang = "de";

    var __ = function (text) {
	var x,y=0,z;
	if (lang.match(/en/i)!=null) return text;
	for (x in translations) {
	    if (lang.match(new RegExp(x,"i"))!=null) {
		for (z in translations.en) {
		    if (text==translations.en[z]) {
			y = z;
			break;
		    }
		}
		return translations[x][y]==undefined?text:translations[x][y];
	    }
	}
	return text;
    };

     $.fn.extend({
	 translate: function () {
	     return this.each(function () {
	     	 $(this).text(__($(this).text()));
	     });
	 }
     });
    
    window.__ = __;

}());
