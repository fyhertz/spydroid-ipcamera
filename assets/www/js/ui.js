(function () {

    //var host = "192.168.0.100",
    var host = window.location.hostname,

    // Encapsulation of the vlc plugin
    Stream = function (object,type,callbacks) {
	     var restarting = false, starting = false, error = false, restartTimer, startTimer,

	     // Register an event listener
	     registerEvent = function (object, event, handler) {
	         if (object.attachEvent) {
		          object.attachEvent (event, handler);
	         } else if (object.addEventListener) {
		          object.addEventListener (event, handler, false);
	         } else {
		          object["on" + event] = handler;
	         }
	     },
	     
	     // Indicates if the camera/microphone is currently being used
	     inUse = function (callback) {
	         $.post('request.json',JSON.stringify({'action':'state'}),function (json) {
		          if (type==='audio') callback(json.state.microphoneInUse==='true');
		          else callback(json.state.cameraInUse==='true');
	         });
	     };

	     registerEvent(object,'MediaPlayerEncounteredError',function (event) {
	         error = true;
	         if (restarting) {
		          clearInterval(restartTimer);
		          restarting = false;
	         }
	         if (starting) {
		          clearInterval(startTimer);
		          starting = false;
	         }	    
	         callbacks.onError(type);
	     });

	     return {

	         restart: function () {
		          if (!restarting) {
		              object.playlist.stop();
		              object.playlist.clear();
		              object.playlist.items.clear();
		              // We wait 2 secs and restart the stream
		              restarting = true;
		              restartTimer = setInterval(function () {
			               inUse(function (b) {
			                   if (!b) {
				                    this.start();
				                    clearInterval(restartTimer);
			                   }
			               }.bind(this));
		              }.bind(this),1000);
		          }
	         },

	         start: function () {
		          if (!object.playlist.isPlaying) {
		              starting = true;
		              error = false;
		              startTimer = setInterval(function () {
			               if (object.playlist.isPlaying) {
			                   restarting = false;
			                   starting = false;
			                   clearInterval(startTimer);
			               }
		              },300);
		              object.playlist.stop();
		              object.playlist.clear();
		              object.playlist.items.clear(); 
		              var item = generateURI(host,type);
		              object.playlist.add(type==='video'?item.uriv:item.uria,'',item.params);
		              object.playlist.playItem(0);
		          }
	         },

	         stop: function () {
		          error = false;
		          if (restarting) {
		              clearInterval(restartTimer);
		              restarting = false;
		          }
		          if (starting) {
		              clearInterval(startTimer);
		              starting = false;
		          }
		          if (object.playlist.isPlaying) {
		              object.playlist.stop();
		              object.playlist.clear();
		              object.playlist.items.clear(); 
		          }
	         },

	         getState: function() {
		          if (restarting) return 'restarting';
		          else if (starting) return 'starting'
		          else if (object.playlist.isPlaying) return 'streaming';
		          else if (error) return 'error';
		          else return 'idle';
	         },

	         isStreaming: function () {
		          return object.playlist.isPlaying;
	         }

	     }
	     
    },

    generateURI = function (h,type) {
	     var audioEncoder, videoEncoder, cache, rotation, flash, camera, res;

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

	     // Camera
	     camera = $('#cameraId').val();

	     // Params
	     cache = /[0-9]+/.exec($('#cache').val())[0];	    

	     return {
	         uria:"rtsp://"+h+":"+8086+"?"+audioEncoder,
	         uriv:"rtsp://"+h+":"+8086+"?"+videoEncoder+'&flash='+flash+'&camera='+camera,
	         params:[':network-caching='+cache]
	     }
    },

    testActivxAndMozillaPlugin = function () {

	     // TODO: console.log(object.VersionInfo);

	     // Test if the activx plugin is installed 
	     if (typeof $('#xvlcv')[0].playlist != "undefined") {
	         return 1;
	     } else {
	         $('#xvlcv').css('display','none');
	         $('#vlcv').css('display','block');
	     }

	     // Test if the mozilla plugin is installed
	     if (typeof $('#vlca')[0].playlist == "undefined") {
	         // Plugin not detected, alert user !
	         $('#glass').fadeIn(1000);
	         $('#error-noplugin').fadeIn(1000);
	         return 0;
	     } else {
	         return 2;
	     }

    },

    loadSoundsList = function (sounds) {
	     var list = $('#soundslist'), category, name;
	     sounds.forEach(function (e) {
	         category = e.match(/([a-z0-9]+)_/)[1];
	         name = e.match(/[a-z0-9]+_([a-z0-9_]+)/)[1];
	         if ($('.category.'+category).length==0) list.append(
                '<div class="category '+category+'"><span class="category-name">'+category+'</span><div class="category-separator"></div></div>'
            );
	         $('.category.'+category).append('<div class="sound" id="'+e+'"><a>'+name.replace(/_/g,' ')+'</a></div>');
	     });
    },

    testScreenState = function (screenState) {
	     if (screenState==0) {
	         $('#error-screenoff').fadeIn(1000);
	         $('#glass').fadeIn(1000);
	     }
    },

    updateTooltip = function (title) {
	     $('#tooltip>div').hide();
	     $('#tooltip #'+title).show();
    },

    videoStream, videoPlugin, audioStream, oldVideoState = 'idle',oldAudioState = 'idle', lastError = 0,

    updateStatus = function () {
	     var status = $('#status'), button = $('#connect>div>h1'), cover = $('#vlc-container #upper-layer'), error;

	     if (videoStream.getState()===oldVideoState && audioStream.getState()===oldAudioState && lastError===0) return;

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
	     if (lastError!==0) {
	         videoPlugin.css('visibility','hidden'); 
	         if (lastError===1) error =  __('Retrieving error message...');
	         else if (lastError===2) error =  __('Connection timed out !');
	         else if (lastError===0 || lastError===undefined) error = "";
	         else error = lastError;
	         lastError = 0;
	         cover.html('<div id="wrapper"><h1>'+__('An error occurred')+' :(</h1><p>'+error+'</p></div>');
	     } else if (videoStream.getState()==='restarting' || audioStream.getState()==='restarting') {
	         videoPlugin.css('visibility','hidden'); 
	         cover.css('background','black').html('<div id="mask"></div><div id="wrapper"><h1>'+__('UPDATING SETTINGS')+'</h1></div>').show();
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

	     oldVideoState = videoStream.getState();
	     oldAudioState = audioStream.getState();

    },

    // Called when an error occurs in spydroid
    onError = function (type) {
	     lastError = 1;
	     $.ajax({type: 'POST', url: 'request.json',data: "[{'action':'state'},{'action':'clear'}]",
		          success: function (json) {
		              lastError = json.state.lastError;
		              try {
			               if (json.lastStackTrace.match("RuntimeException.+MediaStream.start")) {
			                   // If a start failed happened we display additional information
			                   lastError += "<br /><br />"+__("This generally happens when you are trying to use settings that are not supported by your phone.");
			                   $("#quality").click();
			               }
		              } catch (ignore) {}
		              if (json.activityPaused==='0' && type==='video') {
			               testScreenState(0);
		              }
		          },
		          error: function () {
		              lastError = 2;
		          },
		          timeout: 1500
	            });
	     if (videoStream.getState()!=='error') videoStream.stop();
	     if (audioStream.getState()!=='error') audioStream.stop();
    },

    fetchSettings = function (config) {
	     $('#resolution,#framerate,#bitrate,#audioEncoder,#videoEncoder').children().each(function (c) {
		      if ($(this).val()===config.videoResolution || 
		          $(this).val()===config.videoFramerate || 
		          $(this).val()===config.videoBitrate || 
		          $(this).val()===config.audioEncoder ||
		          $(this).val()===config.videoEncoder ) {
		          $(this).parent().children().removeAttr('selected');
		          $(this).attr('selected','true');
		      }
	     });	    
	     if (config.streamAudio===false) $('#audioEnabled').removeAttr('checked');
	     if (config.streamVideo===false) $('#videoEnabled').removeAttr('checked');
    },

    saveSettings = function () {
        var res = /([0-9]+)x([0-9]+)/.exec($('#resolution').val());
        var videoQuality = /[0-9]+/.exec($('#bitrate').val())[0]+'-'+/[0-9]+/.exec($('#framerate').val())[0]+'-'+res[1]+'-'+res[2];		          
        var settings = {
            'stream_video': $('#videoEnabled').attr('checked')=='checked',
            'stream_audio': $('#audioEnabled').attr('checked')=='checked',
            'video_encoder': $('#videoEncoder').val(),
            'audio_encoder': $('#audioEncoder').val(),
            'video_quality': videoQuality
        };
        $.post('request.json',JSON.stringify({'action':'set','settings':settings}));
    },

    // Disable input for one sec to prevent user from flooding the RTSP server by clicking around too quickly
    disableAndEnable = function (input) {
	     input.attr('disabled','true');
	     setTimeout(function () {
	         input.removeAttr('disabled');
	     },1000);
    },

    setupEvents = function () {
	     var audioPlugin, test,
	     cover = $('#vlc-container #upper-layer'),
	     status = $('#status'),
	     button = $('#connect>div>h1');	

	     $('.popup').each(function () {
	         $(this).css({'top':($(window).height()-$(this).height())/2,'left':($(window).width()-$(this).width())/2});
	     });

	     $('.popup #close').click(function (){
	         $('#glass').fadeOut();
	         $('.popup').fadeOut();
	     });
	     
	     test = testActivxAndMozillaPlugin();

	     if (test===1) {
	         // Activx plugin detected
	         videoPlugin = $('#xvlcv');
	         audioPlugin = $('#xvlca');
	     } else if (test===2) {
	         // Mozilla plugin detected
	         videoPlugin = $('#vlcv');
	         audioPlugin = $('#vlca');
	     } else {
	         // No plugin installed, spydroid probably won't work
	         // We assume the Mozilla plugin is installed, just in case :/
	         videoPlugin = $('#vlcv');
	         audioPlugin = $('#vlca');
	     }

	     videoStream = Stream(videoPlugin[0],'video',{onError:function (type) {
	         onError(type);
	     }});	

	     audioStream = Stream(audioPlugin[0],'audio',{onError:function (type) {
	         onError(type);
	     }});

	     setInterval(function () {updateStatus();},400);

	     $('#connect').click(function () {
	         if ($(this).attr('disabled')!==undefined) return;
	         disableAndEnable($(this));
	         if ((videoStream.getState()!=='idle' && videoStream.getState()!=='error') || 
		          (audioStream.getState()!=='idle' && audioStream.getState()!=='error')) {
		          videoStream.stop();
		          audioStream.stop();
	         } else {
		          if (!$('#videoEnabled').attr('checked') && !$('#audioEnabled').attr('checked')) return;
		          videoPlugin.css('visibility','hidden'); 
		          cover.css('background','black').html('<div id="mask"></div><div id="wrapper"><h1>'+__('CONNECTION')+'</h1></div>').show();
		          if ($('#videoEnabled').attr('checked')) videoStream.start(); else videoStream.stop();
		          if ($('#audioEnabled').attr('checked')) audioStream.start();
		          updateStatus();
	         }
	     });
	     
	     $('#torch-button').click(function () {
	         if ($(this).attr('disabled')!==undefined || videoStream.getState()==='starting') return;
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

	     $('.camera-not-selected').live('click',function () {
	         if ($(this).attr('disabled')!==undefined || videoStream.getState()==='starting') return;
	         $('#cameras span').addClass('camera-not-selected');
	         $(this).removeClass('camera-not-selected');
	         disableAndEnable($('.camera-not-selected'));
	         $('#cameraId').val($(this).attr('data-id'));
	         if (videoStream.getState()==='streaming') videoStream.restart();
	     }) 
	     
	     $('.audio select,').change(function () {
	         if (audioStream.isStreaming()) {
		          audioStream.restart();
	         }
	     });

	     $('.audio input').change(function () {
	         if (audioStream.isStreaming() || videoStream.isStreaming()) {
		          if ($('#audioEnabled').attr('checked')) audioStream.restart(); else audioStream.stop();
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
		          if ($('#videoEnabled').attr('checked')) videoStream.restart(); else videoStream.stop();
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

	     $('.sound').live('click', function () {
	         $.post('request.json',JSON.stringify({'action':'play','name':$(this).attr('id')}));
	     });

	     $('#fullscreen').click(function () {
	         videoPlugin[0].video.toggleFullscreen();
	     });

	     $(document).keyup(function(e) { 
	         if (e.keyCode == 27) { 
		          videoPlugin[0].video.toggleFullscreen();
	         }
	     });

	     $('#hide-tooltip').click(function () {
	         $('body').width($('body').width() - $('#tooltip').width());
	         $('#tooltip').hide();
	         $('#need-help').show();
	     });

	     $('#tooltip').hide();
	     $('#need-help').show();

	     $('#need-help').click(function () {
	         $('body').width($('body').width() + $('#tooltip').width());
	         $('#tooltip').show();
	         $('#need-help').hide();
	     });

	 };

    $(document).ready(function () {

        $.post('request.json',"[{'action':'sounds'},{'action':'screen'},{'action':'get'}]", function (data) {

	         // Verify that the screen is not turned off
	         testScreenState(data.screen);
            
	         // Fetch the list of sounds available on the phone
	         loadSoundsList(data.sounds);

	         // Retrieve the configuration of Spydroid on the phone
	         fetchSettings(data.get);
            
        });

	     // Translate the interface in the appropriate language
	     $('h1,h2,h3,span,p,a,em').translate();

	     // Bind DOM events to the js API
	     setupEvents();

    });

}());
