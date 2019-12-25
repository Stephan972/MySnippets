function uncipherContext(decodingContext) {
    /* org.jsoup.nodes.Document */
    var doc = decodingContext.proxiesPage;

    /* org.jsoup.select.Elements */
    var scriptElementsToRemove = doc.select("td script:containsData(document.write)");
    if (scriptElementsToRemove.isEmpty()) {
        decodingContext.warn('No hosts to uncipher found in decoding context.');
    }

    var dummyDocument = {
       host: '',
       write: function(a) {
           this.host = a;
       }
    };
    var jsCode;
    var re = /^document\.write\('[^']+'\.substr\(\d+\)\s+\+\s+'[^']+'\);$/;
    for each( /* org.jsoup.nodes.Element */ script in scriptElementsToRemove) {
        jsCode = script.data();
        var arr = jsCode.match(re);

        decodingContext.debug('\ndata= {}\narr= {}', jsCode, arr.toString());

        if (arr !== null) {
            jsCode = arr[0].replace('document', 'dummyDocument');
            eval(jsCode);
            script.parent().html(dummyDocument.host);
        }
    }
}