document.addEventListener('play', function(audio_selected){
    var audio = document.getElementsByTagName('audio');
    for(var i = 0, len = audio.length; i < len; i++){
        if(audio[i] != audio_selected.target){
            audio[i].pause();
        }
    }
}, true);