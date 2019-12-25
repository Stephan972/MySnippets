function uncipherContext(decodingContext) {
    /* org.jsoup.nodes.Document */
    var doc = decodingContext.proxiesPage;

    /* org.jsoup.select.Elements */
    var scripts = doc.select('script:containsData(chash)');
    var scriptsCount = scripts.size();
    if (scriptsCount != 1) {
        throw 'Invalid scripts count. Expected: 1. Found: ' + scriptsCount;
    }

    var re = /'([^']+)'/;
    var chashScriptElement = scripts.first();
    var arr = chashScriptElement.data().match(re);
    if (arr === null) {
        throw 'Unexpected hash key definition: ' + chashScriptElement.outerHtml();
    }

    var chash = arr[1];
    for each( /* org.jsoup.nodes.Element */ span in doc.select('span.port')) {
    	span.text(decode(span.attr('data-port'), chash));
    }
}

// decode function extracted from ProxyServers.pro website
function decode(cipheredPort, chash) {
	var e = cipheredPort;
	var t = chash;

    for (var n = [], i = 0, r = 0; i < e.length - 1; i += 2, r++) n[r] = parseInt(e.substr(i, 2), 16);
    for (var a = [], i = 0; i < t.length; i++) a[i] = t.charCodeAt(i);
    for (var i = 0; i < n.length; i++) n[i] = n[i] ^ a[i % a.length];
    for (var i = 0; i < n.length; i++) n[i] = String.fromCharCode(n[i]);
    return n = n.join('');
}