(function () {

    var translations = {
	     en: {
	         1:"About",
	         2:"Return",
	         3:"Change quality settings",
	         4:"Flash/Vibrator",
	         5:"Click on the torch to enable or disable the flash",
	         6:"Play a prerecorded sound",
	         7:"Connect !!",
	         8:"Disconnect ?!",
	         9:"STATUS",
	         10:"NOT CONNECTED",
	         11:"ERROR :(",
	         12:"CONNECTION",
	         13:"UPDATING SETTINGS",
	         14:"CONNECTED",
	         15:"Show some tips",
	         16:"Hide those tips",
	         17:"Those buttons will trigger sounds on your phone...",
	         18:"Use them to surprise your victim.",
	         19:"Or you could use this to surprise your victim !",
	         20:"This will simply toggle the led in front of you're phone, so that even in the deepest darkness, you shall not be blind...",
	         21:"If the stream is choppy, try reducing the bitrate or increasing the cache size.",
	         22:"Try it instead of H.263 if video streaming is not working at all !",
	         23:"The H.264 compression algorithm is more efficient but may not work on your phone...",
	         24:"You need to install or update VLC and the VLC mozilla plugin !",
	         25:"During the installation make sure to check the firefox plugin !",
	         26:"Close",
	         27:"You must leave the screen of your smartphone on !",
	         28:"Front facing camera",
	         29:"Back facing camera",
	         30:"Switch between cameras",
	         31:"Streaming video but not audio",
	         32:"Streaming audio but not video",
	         33:"Streaming audio and video",
	         34:"Trying to connect...",
	         35:"Stream sound",
	         36:"Stream video",
	         37:"Fullscreen",
	         38:"Encoder",
	         39:"Resolution",
	         40:"Cache size",
	         41:"This generally happens when you are trying to use settings that are not supported by your phone.",
	         42:"Retrieving error message...",
	         43:"An error occurred",
            44:"Click on the phone to make your phone buzz"
	     },

	     fr: {
	         1:"À propos",
	         2:"Retour",
	         3:"Changer la qualité du stream",
	         4:"Flash/Vibreur",
	         5:"Clique sur l'ampoule pour activer ou désactiver le flash",
	         6:"Jouer un son préenregistré",
	         7:"Connexion !!",
	         8:"Déconnecter ?!",
	         9:"STATUT",
	         10:"DÉCONNECTÉ",
	         11:"ERREUR :(",
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
	         24:"Tu dois d'abord installer ou mettre à jour VLC et le mozilla plugin !",
	         25:"Pendant l'installation laisse cochée l'option plugin mozilla !",
	         26:"Fermer",
	         27:"Tu dois laisser l'écran de ton smartphone allumé",
	         28:"Caméra face avant",
	         29:"Caméra face arrière",
	         30:"Choisir la caméra",
	         31:"Streaming de la vidéo",
	         32:"Streaming de l'audio",
	         33:"Streaming de l'audio et de la vidéo",
	         34:"Connexion en cours...",
	         35:"Streaming du son",
	         36:"Streaming de la vidéo",
	         37:"Plein écran",
	         38:"Encodeur",
	         39:"Résolution",
	         40:"Taille cache",
	         41:"En général, cette erreur se produit quand les paramètres sélectionnés ne sont pas compatibles avec le smartphone.",
	         42:"Attente du message d'erreur...",
	         43:"Une erreur s'est produite",
            44:"Clique pour faire vibrer ton tel."
	     },

	     ru: {
	         1:"Спасибо",
	         2:"Вернуться",
	         3:"Изменить настройки качества",
	         4:"Переключатель вспышки",
	         5:"Нажмите здесь, чтобы включить или выключить вспышку",
	         6:"Проиграть звук на телефоне",
	         7:"Подключиться !!",
	         8:"Отключиться ?!",
	         9:"СОСТОЯНИЕ",
	         10:"НЕТ ПОДКЛЮЧЕНИЯ",
	         11:"ОШИБКА :(",
	         12:"ПОДКЛЮЧЕНИЕ",
	         13:"ОБНОВЛЕНИЕ НАСТРОЕК",
	         14:"ПОДКЛЮЧЕНО",
	         15:"Показать поясняющие советы",
	         16:"Спрятать эти советы",
	         17:"Эти кнопки будут проигрывать звуки на вашем телефоне...",
	         18:"Используйте их, чтобы удивить вашу жертву.",
	         19:"Или вы можете удивить свою жертву!",
	         20:"Это переключатель режима подсветки на передней части вашего телефона, так что даже в самой кромешной тьме, вы не будете слепы ...",
	         21:"Если поток прерывается, попробуйте уменьшить битрейт или увеличив размер кэша.",
	         22:"Если топоковое видео не работает совсем, попробуйте сжатие Н.263",
	         23:"Алгоритм сжатия H.264, является более эффективным, но может не работать на вашем телефоне ...",
	         24:"You need to install or update VLC and the VLC mozilla plugin !",
	         25:"При установке убедитесь в наличии плагина для firefox !",
	         26:"Закрыть",
	         27:"Вам надо отойти от вашего смартфона.",
	         28:"Фронтальная камера",
	         29:"Камера с обратной стороны",
	         30:"Переключиться на другую камеру",
	         31:"Передача видео без звука",
	         32:"Передача звука без видео",
	         33:"Передача звука и видео",
	         34:"Пытаемся подключится",
	         35:"Аудио поток",
	         36:"Видео поток",
	         37:"На весь экран",
	         38:"Кодек",
	         39:"Разрешение",
	         40:"Размер кеша",
	         41:"Как правило, это происходит, когда вы пытаетесь использовать настройки, не поддерживаемые вашим телефоном.",
	         42:"Получение сообщения об ошибке ..."
	     },

	     de : {
	         1:"Apropos",
	         2:"Zurück",
	         3:"Qualität des Streams verändern",
	         4:"Fotolicht ein/aus",
	         5:"Klick die Glühbirne an, um das Fotolicht einzuschalten oder azufallen",
	         6:"Vereinbarten Ton spielen",
	         7:"Verbindung !!",
	         8:"Verbinden ?!",
	         9:"STATUS",
	         10:"NICHT VERBUNDEN",
	         11:"FEHLER :(",
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
	         24:"You need to install or update VLC and the VLC mozilla plugin !",
	         25:"Während der Installation, prüfe dass das Firefox plugin abgecheckt ist!",
	         26:"Zumachen",
	         27:"Du musst den Bildschirm deines Smartphones eingeschaltet lassen !",
	         28:"Frontkamera",
	         29:"Rückkamera",
	         30:"Kamera auswählen",
	         31:"Videostreaming",
	         32:"Audiostreaming",
	         33:"Video- und Audiostreaming",
	         34:"Ausstehende Verbindung...",
	         35:"Soundstreaming",
	         36:"Videostreaming",
	         37:"Ganzer Bildschirm",
	         38:"Encoder",
	         39:"Auflösung",
	         40:"Cachegröße",
	         41:"Dieser Fehler gescheht überhaupt, wenn die gewählten Einstellungen mit dem Smartphone nicht kompatibel sind.",
	         42:"Es wird auf die Fehlermeldung gewartet...",
	         43:"Ein Fehler ist geschehen..."    
	     }
    };

    var lang = window.navigator.userLanguage || window.navigator.language;
    //var lang = "ru";

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
	     	       $(this).html(__($(this).html()));
	         });
	     }
    });
    
    window.__ = __;

}());
