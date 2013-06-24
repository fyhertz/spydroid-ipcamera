(function () {

    var host = window.location.hostname;
    var port = window.location.port;
    var videoPlugin;
    var videoStream;
    var audioPlugin;
    var audioStream;
    var error;
    var volume;

    function stream(object,type,done) {

	     var state = "idle";
        
	     return {

	         restart: function () {
                state = "restarting";
                done();
		          if (object.playlist.isPlaying) {
		              object.playlist.stop();
		              object.playlist.clear();
		              object.playlist.items.clear();
                    setTimeout(function () {
                        this.start(false);
                    }.bind(this),2000);
		          } else {
                    this.start(false);
                }
	         },

	         start: function (e) {
                var req = generateUriParams(type);
                state = "starting";
                if (e!==false) done();
                $.ajax({
                    type: 'GET', 
                    url: 'spydroid.sdp?id='+(type==='video'?0:1)+'&'+req.uri, 
                    success: function (e) {
                        setTimeout(function () {
                            state = "streaming";
                            object.playlist.add('http://'+host+':'+port+'/spydroid.sdp?id='+(type==='video'?0:1)+'&'+req.uri,'',req.params);
		                      object.playlist.playItem(0);
                            setTimeout(function () {
                                done();
                            },600);
                        },1000);
                    }, 
                    error: function () {
                        state = "error";
                        getError();
                    }
                });
		      },

	         stop: function () {
                $.ajax({
                    type: 'GET', 
                    url: 'spydroid.sdp?id='+(type==='video'?0:1)+'&stop', 
                    success: function (e) {
                        //done(); ??
                    }, 
                    error: function () {
                        state = "error";
                        getError();
                    }
                });
		          if (object.playlist.isPlaying) {
		              object.playlist.stop();
		              object.playlist.clear();
		              object.playlist.items.clear();               
		          }
                state = "idle";
                done();
	         },

	         getState: function() {
                return state;
	         },

	         isStreaming: function () {
		          return object.playlist.isPlaying;
	         }

	     }
	     
    }

    function sendRequest(request,success,error) {
        var data;
        if (typeof request === "string") data = {'action':request}; else data = request;
        $.ajax({type: 'POST', url: 'request.json', data: JSON.stringify(data), success:success, error:error});
    }

    function updatePhoneStatus(data) {

        if (data.volume !== undefined) {
            volume = data.volume;
            $('#sound>#volume').text(volume.current);
        }
        $('#battery>#level').text(data.battery);
        
        setTimeout(function () {
	         sendRequest(
                [{"action":"battery"},{"action":"volume"}],
                function (e) {
                    updatePhoneStatus(e);
                },
                function () {
                    updatePhoneStatus({battery:'??'});
                }
            );
        },100000);
    }

    function updateTooltip(title) {
	     $('#tooltip>div').hide();
	     $('#tooltip #'+title).show();
    }
    
    function loadSettings(config) {
	     $('#resolution,#framerate,#bitrate,#audioEncoder,#videoEncoder').children().each(function (c) {
		      if ($(this).val()===config.videoResolution || 
		          $(this).val()===config.videoFramerate || 
		          $(this).val()===config.videoBitrate || 
		          $(this).val()===config.audioEncoder ||
		          $(this).val()===config.videoEncoder ) {
		          $(this).parent().children().prop('selected',false);
		          $(this).prop('selected',true);
		      }
	     });	    
	     if (config.streamAudio===false) $('#audioEnabled').prop('checked',false);
	     if (config.streamVideo===false) $('#videoEnabled').prop('checked',false);
    }

    function saveSettings() {
        var res = /([0-9]+)x([0-9]+)/.exec($('#resolution').val());
        var videoQuality = /[0-9]+/.exec($('#bitrate').val())[0]+'-'+/[0-9]+/.exec($('#framerate').val())[0]+'-'+res[1]+'-'+res[2];		          
        var settings = {
            'stream_video': $('#videoEnabled').prop('checked')===true,
            'stream_audio': $('#audioEnabled').prop('checked')===true,
            'video_encoder': $('#videoEncoder').val(),
            'audio_encoder': $('#audioEncoder').val(),
            'video_quality': videoQuality
        };
        sendRequest({'action':'set','settings':settings});
    }

    function loadSoundsList(sounds) {
	     var list = $('#soundslist'), category, name;
	     sounds.forEach(function (e) {
	         category = e.match(/([a-z0-9]+)_/)[1];
	         name = e.match(/[a-z0-9]+_([a-z0-9_]+)/)[1];
	         if ($('.category.'+category).length==0) list.append(
                '<div class="category '+category+'"><span class="category-name">'+category+'</span><div class="category-separator"></div></div>'
            );
	         $('.category.'+category).append('<div class="sound" id="'+e+'"><a>'+name.replace(/_/g,' ')+'</a></div>');
	     });
    }

    function testScreenState(state) {
	     if (state===0) {
	         $('#error-screenoff').fadeIn(1000);
	         $('#glass').fadeIn(1000);
	     }
    }

    function testVlcPlugin() {
        if (videoPlugin[0].VersionInfo === undefined || videoPlugin[0].VersionInfo.indexOf('2.0') === -1) {
            $('#error-noplugin').fadeIn();
	         $('#glass').fadeIn();
        }
    }

    function generateUriParams(type) {
	     var audioEncoder, videoEncoder, cache, rotation, flash, camera, res;

	     // Audio conf
	     if ($('#audioEnabled').prop('checked')) {
	         audioEncoder = $('#audioEncoder').val()=='AMR-NB'?'amr':'aac';
	     } else {
	         audioEncoder = "nosound";
	     }
	     
	     // Resolution
	     res = /([0-9]+)x([0-9]+)/.exec($('#resolution').val());

	     // Video conf
	     if ($('#videoEnabled').prop('checked')) {
	         videoEncoder = ($('#videoEncoder').val()=='H.263'?'h263':'h264')+'='+
		          /[0-9]+/.exec($('#bitrate').val())[0]+'-'+
		          /[0-9]+/.exec($('#framerate').val())[0]+'-';
	         videoEncoder += res[1]+'-'+res[2];
	     } else {
	         videoEncoder = "novideo";
	     }
	     
	     // Flash
	     if ($('#flashEnabled').val()==='1') flash = 'on'; else flash = 'off';

	     // Camera
	     camera = $('#cameraId').val();

	     // Params
	     cache = /[0-9]+/.exec($('#cache').val())[0];	    

	     return {
	         uri:type==='audio'?audioEncoder:(videoEncoder+'&flash='+flash+'&camera='+camera),
	         params:[':network-caching='+cache]
	     }
    }

    function getError() {
        sendRequest(
            'state',
            function (e) {
                error = e.state.lastError;
                updateStatus();
            },
            function () {
                error = 'Phone unreachable !';
                updateStatus();
            }         
        );
    }

     function updateStatus() {
	     var status = $('#status'), button = $('#connect>div>h1'), cover = $('#vlc-container #upper-layer');

	     // STATUS
	     if (videoStream.getState()==='starting' || videoStream.getState()==='restarting' || 
	         audioStream.getState()==='starting' || audioStream.getState()==='restarting') {
	         status.html(__('Trying to connect...'))
	     } else {
	         if (!videoStream.isStreaming() && !audioStream.isStreaming()) status.html(__('NOT CONNECTED')); 
	         else if (videoStream.isStreaming() && !audioStream.isStreaming()) status.html(__('Streaming video but not audio'));
	         else if (!videoStream.isStreaming() && audioStream.isStreaming()) status.html(__('Streaming audio but not video'));
	         else status.html(__('Streaming audio and video'));
	     }

	     // BUTTON
	     if ((videoStream.getState()==='idle' || videoStream.getState()==='error') && 
	         (audioStream.getState()==='idle' || audioStream.getState()==='error')) {
	         button.html(__('Connect !!')); 
	     } else button.text(__('Disconnect ?!'));

	     // WINDOW
	     if (videoStream.getState()==='error' || audioStream.getState()==='error') {
	         videoPlugin.css('visibility','hidden');
	         cover.html('<div id="wrapper"><h1>'+__('An error occurred')+' :(</h1><p>'+error+'</p></div>');
	     } else if (videoStream.getState()==='restarting' || audioStream.getState()==='restarting') {
	         videoPlugin.css('visibility','hidden'); 
	         cover.css('background','black').html('<div id="mask"></div><div id="wrapper"><h1>'+__('UPDATING SETTINGS')+'</h1></div>').show();
        } else if (videoStream.getState()==='starting' || audioStream.getState()==='starting') {
		      videoPlugin.css('visibility','hidden'); 
            cover.css('background','black').html('<div id="mask"></div><div id="wrapper"><h1>'+__('CONNECTION')+'</h1></div>').show();
	     } else if (videoStream.getState()==='streaming') {
	         videoPlugin.css('visibility','inherit');
	         cover.hide();
	     }

	     if (videoStream.getState()==='idle') {
	         if (audioStream.getState()==='streaming') {
		          videoPlugin.css('visibility','hidden'); 
		          cover.html('').css('background','url("images/speaker.png") center no-repeat').show();
	         } else if (audioStream.getState()==='idle') {
		          videoPlugin.css('visibility','hidden'); 
		          cover.html('').css('background','url("images/eye.png") center no-repeat').show();
	         }
	     }

    }

    function disableAndEnable(input) {
	     input.prop('disabled',true);
	     setTimeout(function () {
	         input.prop('disabled',false);
	     },1000);
    }

    function setupEvents() {

	     var cover = $('#vlc-container #upper-layer');
	     var status = $('#status');
	     var button = $('#connect>div>h1');	

	     $('.popup #close').click(function (){
	         $('#glass').fadeOut();
	         $('.popup').fadeOut();
	     });
	    
	     $('#connect').click(function () {
	         if ($(this).prop('disabled')===true) return;
	         disableAndEnable($(this));
	         if ((videoStream.getState()!=='idle' && videoStream.getState()!=='error') || 
		          (audioStream.getState()!=='idle' && audioStream.getState()!=='error')) {
		          videoStream.stop();
		          audioStream.stop();
	         } else {
		          if (!$('#videoEnabled').prop('checked') && !$('#audioEnabled').prop('checked')) return;
		          if ($('#videoEnabled').prop('checked')) videoStream.start();
		          if ($('#audioEnabled').prop('checked')) audioStream.start();
	         }
	     });
	     
	     $('#torch-button').click(function () {
	         if ($(this).prop('disabled')===true || videoStream.getState()==='starting') return;
	         disableAndEnable($(this));
	         if ($('#flashEnabled').val()=='0') {
		          $('#flashEnabled').val('1');
		          $(this).addClass('torch-on');
	         } else {
		          $('#flashEnabled').val('0');
		          $(this).removeClass('torch-on');
	         }
	         if (videoStream.getState()==='streaming') videoStream.restart();
	     });

	     $('#buzz-button').click(function () {
            $(this).animate({'padding-left':'+=10'}, 40, 'linear')
                .animate({'padding-left':'-=20'}, 80, 'linear')
                .animate({'padding-left':'+=10'}, 40, 'linear');
            sendRequest('buzz');
        });

	     $(document).on('click', '.camera-not-selected', function () {
	         if ($(this).prop('disabled')===true || videoStream.getState()==='starting') return;
	         $('#cameras span').addClass('camera-not-selected');
	         $(this).removeClass('camera-not-selected');
	         disableAndEnable($('.camera-not-selected'));
	         $('#cameraId').val($(this).attr('data-id'));
	         if (videoStream.getState()==='streaming') videoStream.restart();
	     }) 
	     
	     $('.audio select').change(function () {
	         if (audioStream.isStreaming()) {
		          audioStream.restart();
	         }
	     });

	     $('.audio input').change(function () {
	         if (audioStream.isStreaming() || videoStream.isStreaming()) {
		          if ($('#audioEnabled').prop('checked')) audioStream.restart(); else audioStream.stop();
		          disableAndEnable($(this));
	         }
	     });

	     $('.video select').change(function () {
	         if (videoStream.isStreaming()) {
		          videoStream.restart();
	         }
	     });

	     $('.video input').change(function () {
	         if (audioStream.isStreaming() || videoStream.isStreaming()) {
		          if ($('#videoEnabled').prop('checked')) videoStream.restart(); else videoStream.stop();
		          disableAndEnable($(this));
	         }
	     });

	     $('.cache select').change(function () {
	         if (videoStream.isStreaming()) {
		          videoStream.restart();
	         }
	         if (audioStream.isStreaming()) {
		          audioStream.restart();
	         }
	     });

        $('select,input').change(function () {
            saveSettings();
        });

	     $('.section').click(function () {
	         $('.section').removeClass('selected');
	         $(this).addClass('selected');
	         updateTooltip($(this).attr('id'));
	     });

	     $(document).on('click', '.sound', function () {
            sendRequest([{'action':'play','name':$(this).attr('id')}]);
	     });

	     $('#fullscreen').click(function () {
	         videoPlugin[0].video.toggleFullscreen();
	     });

	     $('#hide-tooltip').click(function () {
	         $('body').width($('body').width() - $('#tooltip').width());
	         $('#tooltip').hide();
	         $('#need-help').show();
	     });

	     $('#need-help').click(function () {
	         $('body').width($('body').width() + $('#tooltip').width());
	         $('#tooltip').show();
	         $('#need-help').hide();
	     });

        $('#sound #plus').click(function () {
            volume.current += 1;
            if (volume.current > volume.max) volume.current = volume.max;
            else sendRequest([{'action':'volume','set':volume.current}]);            
            $('#sound>#volume').text(volume.current);
        });

        $('#sound #minus').click(function () {
            volume.current -= 1;
            if (volume.current < 0) volume.current = 0;
            else sendRequest([{'action':'volume','set':volume.current}]);
            $('#sound>#volume').text(volume.current);
        });

        window.onbeforeunload = function (e) {
            videoStream.stop();
            audioStream.stop();
        }

	 };

    
    $(document).ready(function () {
     
        videoPlugin = $('#vlcv');
        videoStream = stream(videoPlugin[0],'video',updateStatus);
        audioPlugin = $('#vlca');
        audioStream = stream(audioPlugin[0],'audio',updateStatus);

        testVlcPlugin();

        sendRequest([{'action':'sounds'},{'action':'screen'},{'action':'get'},{'action':'battery'},{'action':'volume'}], function (data) {

	         // Verifies that the screen is not turned off
	         testScreenState(data.screen);
            
	         // Fetches the list of sounds available on the phone
	         loadSoundsList(data.sounds);

	         // Retrieves the configuration of Spydroid on the phone
	         loadSettings(data.get);

            // Retrieves volume and battery level
            updatePhoneStatus(data);
            
        });

	     // Translate the interface in the appropriate language
	     $('h1,h2,h3,span,p,a,em').translate();

	     $('.popup').each(function () {
	         $(this).css({'top':($(window).height()-$(this).height())/2,'left':($(window).width()-$(this).width())/2});
	     });

	     $('#tooltip').hide();
	     $('#need-help').show();

	     // Bind DOM events
	     setupEvents();

    });


}());
