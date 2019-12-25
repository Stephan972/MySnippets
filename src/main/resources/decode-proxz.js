function uncipherContext(decodingContext) {
    // # Unciper proxies IPs
    /* org.jsoup.nodes.Document */
    var doc = decodingContext.proxiesPage;

    /* org.jsoup.select.Elements */
    var scriptElementsToUncipher = doc.select("td > script:containsData(eval\\(unescape\\()");
    if (scriptElementsToUncipher.isEmpty()) {
        decodingContext.warn('No hosts to uncipher found in decoding context.');
    }    

    var re=/[\s\S]*?eval.+%22(.+)%22[\s\S]*/;
    for each( /* org.jsoup.nodes.Element */ script in scriptElementsToUncipher) {
        var escapedProxyIP = script.data().replace(re, '$1');
        script.parent().text(unescape(escapedProxyIP));
    }
    
    // # Uncipher next page to fetch
    var documentLocation = doc.location();
    if ( documentLocation && (documentLocation !== '') ) {
        decodingContext.info(documentLocation);

        var anchorNext = doc.selectFirst('body > div > center > table > tbody > tr:nth-child(4) > td:nth-child(3) > font:nth-child(6) > a[abs:href*='+documentLocation+'] + a');
        if (!anchorNext) {
            decodingContext.info('Last page detected.');
        } else {
            anchorNext.attr('id', '__proxz_next__');
        }        
    } else {
        decodingContext.info('Document location is empty or null. No next page detected.');
    }
}