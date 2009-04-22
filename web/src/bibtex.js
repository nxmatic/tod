function load_bibtex(author){
     var url = '/bibtex/bibtex_proxy.php?a=' + author;
     var myAjax = new Ajax.Updater('bibtex', url, {method: 'get'});
}

function load_bibtex_tod(){
     load_bibtex('tod');
}

function bibtex(key){
     var escapedKey = key.replace(/:/, "-");
     window.location = 'http://pleiad.dcc.uchile.cl/research/publications?key=' + escapedKey;
}

