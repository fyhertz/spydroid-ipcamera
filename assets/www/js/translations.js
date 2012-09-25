(function () {

    var translations = {
	fr: {
	    "About":"À propos",
	    "Return":"Retour",
	    "Change quality settings":"Changer la qualité du stream",
	    "Toggle flash":"Allumer/Éteindre le flash",
	    "Click on the torch to enable or disable the flash":"Clique sur l'ampoule pour activer ou désactiver le flash",
	    "Play a prerecorded sound":"Jouer un son préenregistré",
	    "Connect !!":"Connection !!",
	    "Deconnect ?!":"Déconnecter ?!",
	    "STATUS":"STATUT",
	    "NOT CONNECTED":"DÉCONNECTÉ",
	    "ERROR":"ERREUR",
	    "CONNEXION":"CONNECTION",
	    "UPDATING SETTINGS":"M.A.J.",
	    "CONNECTED":"CONNECTÉ",
	    "Show some tips":"Afficher l'aide",
	    "Hide those tips":"Masquer l'aide",
	    "Those buttons will trigger sounds on your phone...":"Clique sur un de ces boutons pour lancer un son préenregistré sur ton smartphone !",
	    "Use them to surprise your victim.":"Utilise les pour surprendre ta victime !!",
	    "Or you could use this to surprise your victim !":"Ça peut aussi servir à surprendre ta victime !",
	    "This will simply toggle the led in front of you're phone, so that even in the deepest darkness, you will not be blind...":"Clique sur l'ampoule pour allumer le flash de ton smartphone",
	    "If the stream is choppy, try reducing the bitrate or increasing the cache size.":"Si le stream est saccadé essaye de réduire le bitrate ou d'augmenter la taille du cache.",
	    "Try it instead of H.263 if video streaming is not working at all !":"Essaye le à la place du H.263 si le streaming de la vidéo ne marche pas du tout !",
	    "The H.264 compression algorithm is more efficient but may not work on your phone...":"Le H.264 est un algo plus  efficace pour compresser la vidéo mais il a moins de chance de marcher sur ton smartphone...",
	    "You need to install VLC first !":"Tu dois d'abord installer VLC !!",
	    "During the installation make sure to check the firefox plugin !":"Pendant l'installation laisse cochée l'option plugin mozilla !",
	    "Close":"Fermer",
	    "You must leave the screen of your smartphone on !":"Tu dois laisser l'écran de ton smartphone allumé"
	}
    };

    var lang = window.navigator.userLanguage || window.navigator.language;
    //var lang = "en";

    var __ = function (text) {
	if (lang.match(/en/i)!=null) return text;
	for (x in translations) {
	    if (lang.match(new RegExp(x,"i"))!=null) {
		return translations[x][text]==undefined?text:translations[x][text];
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