(function () {

    //var host = "192.168.0.105",
    var host = /(.+):/.exec(window.location.host)[1],

    generateURI = function (h) {
	var audioEncoder, videoEncoder, cache, rotation, flash, res;

	// Audio conf
	if ($('#audioEnabled').attr('checked')) {
	    audioEncoder = $('#audioEncoder').val()=='AMR-NB'?'amr':'aac';
	} else {
	    audioEncoder = "nosound";
	}
	
	// Resolution
	res = /([0-9]+)x([0-9]+)/.exec($('#resolution').val());

	// Video conf
	if ($('#videoEnabled').attr('checked')) {
	    videoEncoder = ($('#videoEncoder').val()=='H.263'?'h263':'h264')+'='+
		/[0-9]+/.exec($('#bitrate').val())[0]+'-'+
		/[0-9]+/.exec($('#framerate').val())[0]+'-';
	    videoEncoder += res[1]+'-'+res[2];
	} else {
	    videoEncoder = "novideo";
	}
	
	// Flash
	if ($('#flashEnabled').val()==='1') flash = 'on'; else flash = 'off';

	// Params
	cache = /[0-9]+/.exec($('#cache').val())[0];
	
	$.get('config.json?set&'+videoEncoder+'&'+audioEncoder);

	return {
	    uria:"rtsp://"+h+":"+8086+"?"+audioEncoder,
	    uriv:"rtsp://"+h+":"+8086+"?"+videoEncoder+'&flash='+flash,
	    params:[':network-caching='+cache]
	}
    },
    
    Vlc = function () {
	var vlcv = $('#xvlcv'), vlca = $('#xvlca'), playerv = vlcv[0], playera = vlca[0],
	connected = false, restarting = false,
	cover = $('#vlc-container #upper-layer'),
	status = $('#status'),
	button = $('#connect>div>h1');
	video = false, audio = false;

	// No activex plugin ? 
	if (typeof playerv.playlist == "undefined") {
	    vlcv.hide();
	    vlcv = $('#vlcv');
	    vlca = $('#vlca');
	    vlcv.show();
	    playerv = vlcv[0];
	    playera = vlca[0];
	}

	// No firefox plugin ?
	if (typeof playerv.playlist == "undefined") {
	    $('#glass').fadeIn(1000);
	    $('#error-noplugin').fadeIn(1000);
	}

	var registerEvent = function (player, event, handler) {
	    if (player.attachEvent) {
		player.attachEvent (event, handler);
	    } else if (player.addEventListener) {
		player.addEventListener (event, handler, false);
	    } else {
		player["on" + event] = handler;
	    }
	},

	init = function () {
	    vlcv.css('visibility','hidden');
	    cover.html('').css('background','url("images/eye.png") center no-repeat').show();
	},

	stop = function (what) {
	    if (what == undefined || what == "video") {
		playerv.playlist.stop();
		playerv.playlist.clear();
		playerv.playlist.items.clear(); // Not working very well :/
		video = false;
	    }
	    if (what == undefined || what == "audio") {
		playera.playlist.stop();
		playera.playlist.clear();
		playera.playlist.items.clear();
		audio = false;
	    }
	    connected = video || audio;
	    if (!connected && !restarting) {
		init();
		status.html(__('NOT CONNECTED')); 
		button.html(__('Connect !!')); 
	    }
	},

	error = function () {
	    cover.html('<h1>'+__('ERROR')+' :(</h1>');
	    status.html(__('ERROR'));
	    button.text(__('Connect !!'));
	    connected = false;
	},

	addCallback = function (vlc,f) {
	    var t = setInterval(function () {
		if (vlc.playlist.isPlaying) {
		    clearInterval(t);
		    f();
		}
	    },100);
	};

	registerEvent(playera,'MediaPlayerEncounteredError',function (event) {
	    error();
	});
	registerEvent(playerv,'MediaPlayerEncounteredError',function (event) {
	    error();
	});
	registerEvent(playerv,'MediaPlayerPlaying',function (event) {	   
	    // Do not work well
	});
	registerEvent(playera,'MediaPlayerPlaying',function (event) {	    
	    // Do not work well	    
	});
	registerEvent(playerv,'MediaPlayerBuffering',function (event) {
	    // Do not work well
	});
	
	return {

	    init: function () {
		init();
	    },

	    play: function (what) {
		var item = generateURI(host);
		connected = true;
		if (!restarting) {
		    vlcv.css('visibility','hidden');
		    cover.css('background','black').html('<div id="mask"></div><h1>'+__('CONNECTION')+'</h1>').show();
		}
		if (what == "video") {
		    addCallback(playerv,function () {
			cover.hide();
			status.html(__('CONNECTED'));
			video = true;
			button.text(__('Disconnect ?!'));
			vlcv.css('visibility','inherit');
		    });
		    playerv.playlist.add(item.uriv,'',item.params);
		    playerv.playlist.playItem(0);
		}
		if (what == "audio") {
		    addCallback(playera,function () {
			cover.hide();
			vlcv.css('visibility','inherit');
			status.html(__('CONNECTED'));
			audio = true;
			button.text(__('Disconnect ?!'));
		    });
		    playera.playlist.add(item.uria,'',item.params);
		    playera.playlist.playItem(0);
		}
	    },

	    stop: function (what) {
		stop(what);
	    },

	    restart: function (what) {
		if (!restarting) {
		    restarting = true;
		    vlcv.css('visibility','hidden');
		    cover.css('background','black').html('<div id="mask"></div><h1>'+__('UPDATING SETTINGS')+'</h1>').show();
		    stop(what);
		    setTimeout(function () {
			this.play(what);
			restarting = false;
		    }.bind(this),2000);
		}
	    },

	    isStreamingVideo: function() {
		return video;
	    },

	    isStreamingAudio: function() {
		return audio;
	    },

	    isConnected: function() {
		return connected;
	    }
	    
	}

    },

    displaySoundsList = function () {
	var list = $('#soundslist'), category, name;
	sounds.forEach(function (e) {
	    category = e.match(/([a-z0-9]+)_/)[1];
	    name = e.match(/[a-z0-9]+_([a-z0-9_]+)/)[1];
	    if ($('.category.'+category).length==0) list.append('<div class="category '+category+'"><span class="category-name">'+category+'</span><div class="category-separator"></div></div>');
	    $('.category.'+category).append('<div class="sound" id="'+e+'">'+name.replace(/_/g,' ')+'</div>');
	});
    },

    testScreenState = function () {
	if (screenState==0) {
	    $('#error-screenoff').fadeIn(1000);
	    $('#glass').fadeIn(1000);
	}
    },

    updateTooltip = function (title) {
	$('#tooltip>div').hide();
	$('#tooltip #'+title).show();
    },

    loadSpydroidUI = function () {
	var vlc = Vlc(), wait = false;

	vlc.init();

	$('h1,h2,span,p').translate();

	$('#connect').click(function () {
	    if (!wait) {
		wait = true;
		setTimeout(function () {wait = false;},1000);
		if (!vlc.isConnected()) {
		    if ($('#videoEnabled').attr('checked')) vlc.play('video');
		    if ($('#audioEnabled').attr('checked')) vlc.play('audio');
		} else vlc.stop();
	    }
	});
	
	$('#torch-button').click(function () {
	    if ($('#flashEnabled').val()=='0') {
		$('#flashEnabled').val('1');
		$(this).addClass('torch-on');
	    } else {
		$('#flashEnabled').val('0');
		$(this).removeClass('torch-on');
	    }
	    if (vlc.isStreamingVideo()) vlc.restart("video");
	});

	$('.audio select,').change(function () {
	    if (vlc.isStreamingAudio()) {
		vlc.restart("audio");
	    }
	});

	$('.audio input').change(function () {
	    if (vlc.isConnected()) {
		if ($('#audioEnabled').attr('checked')) vlc.play('audio'); else vlc.stop('audio');
	    }
	});

	$('.video select').change(function () {
	    if (vlc.isStreamingVideo()) {
		vlc.restart("video");
	    }
	});

	$('.video input').change(function () {
	    if (vlc.isConnected()) {
		if ($('#videoEnabled').attr('checked')) vlc.play('video'); else vlc.stop('video');
	    }
	});

	$('.cache select').change(function () {
	    if (vlc.isStreamingVideo()) {
		vlc.restart("video");
	    }
	    if (vlc.isStreamingAudio()) {
		vlc.restart("audio");
	    }
	});

	$('.section').click(function () {
	    $('.section').removeClass('selected');
	    $(this).addClass('selected');
	    updateTooltip($(this).attr('id'));
	});

	testScreenState();
	displaySoundsList();

	$('.sound').click(function () {
	    $.get('/sound.htm?name='+$(this).attr('id'));
	});

	$('#hide-tooltip').click(function () {
	    $('body').width($('body').width() - $('#tooltip').width());
	    $('#tooltip').hide();
	    $('#need-help').show();
	});

	//$('body').width($('body').width() + $('#tooltip').width());
	$('#tooltip').hide();
	$('#need-help').show();

	$('#need-help').click(function () {
	    $('body').width($('body').width() + $('#tooltip').width());
	    $('#tooltip').show();
	    $('#need-help').hide();
	});

	$('.popup #close').click(function (){
	    $('#glass').fadeOut();
	    $('.popup').fadeOut();
	});

	$('.popup').css({'top':($(window).height()-$('.popup').height())/2,'left':($(window).width()-$('.popup').width())/2});

	$.getJSON('config.json?get',function (config) {
	    $('#resolution,#framerate,#bitrate,#audioEncoder,#videoEncoder').children().removeAttr('selected').each(function (c) {
		if ($(this).val()===config.videoResolution || 
		    $(this).val()===config.videoFramerate || 
		    $(this).val()===config.videoBitrate || 
		    $(this).val()===config.audioEncoder ||
		    $(this).val()===config.videoEncoder ) {
		    $(this).attr('selected','true');
		}
	    });	    
	    if (config.streamAudio===false) $('#audioEnabled').removeAttr('checked');
	    if (config.streamVideo===false) $('#videoEnabled').removeAttr('checked');
	});

    };

    $(document).ready(function () {
	loadSpydroidUI();
    });

}());
