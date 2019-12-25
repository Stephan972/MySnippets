!function(w) {
    // # Ajax requests handling
    w.__hookAjaxContext = {};
    w.__hookAjaxContext.expectedAjaxRequests = [];

    w.startMonitoringAjaxRequest = function(method, url) {
        var id = 'ajax_response_' + new Date().getTime();

        __hookAjaxContext.expectedAjaxRequests.push({
            expectedMethod : method.toUpperCase(),
            expectedUrl : url.toLowerCase(),
            id : id
        });

        return id;
    };

    w.handleAjaxResponse = function(xhr, id) {
        window.injectScriptInfo(id, 'text/ajax-response', xhr.responseText);
    };

    w.injectScriptInfo = function(id, type, text) {
        var scriptInfo = document.createElement('script');
        scriptInfo.id = id;
        scriptInfo.type = type;
        scriptInfo.text = text;
        document.head.appendChild(scriptInfo);        
    }
    
    w.stopMonitoringAjaxRequest = function(id) {
        var ctx = __hookAjaxContext;
        var ear = ctx.expectedAjaxRequests;

        var len = ear.length;
        for (var i = 0; i < len; i++) {
            var expectedAjaxRequest = ear[i];
            if (expectedAjaxRequest.id === id) {
                var ajaxResponseElement = document.getElementById(id);
                if (ajaxResponseElement && ajaxResponseElement.parentNode) {
                    ajaxResponseElement.parentNode
                            .removeChild(ajaxResponseElement);
                }

                ear.splice(i, 1);
                break;
            }
        }
    };

    w.hookAjax({
        // hook callbacks
        onload : function(xhr) {
            var ctx = __hookAjaxContext;
            var ear = ctx.expectedAjaxRequests;

            var len = ear.length;
            for (var i = 0; i < len; i++) {
                var expectedAjaxRequest = ear[i];
                if ((expectedAjaxRequest.expectedMethod === xhr.__method)
                        && (expectedAjaxRequest.expectedUrl === xhr.__originalURL)) {
                    window.handleAjaxResponse(xhr,
                            expectedAjaxRequest.id);
                }
            }
        },

        // hook function
        open : function(arg) {
            if (__hookAjaxContext.expectedAjaxRequests.length > 0) {
                this.__method = arg[0].toUpperCase();
                this.__originalURL = arg[1].toLowerCase();
                // __async == arg[2]
            }
        }
    });
}(window);