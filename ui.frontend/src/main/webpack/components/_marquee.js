// Applies the authored loop duration to the marquee as a CSS custom property.
// The stylesheet ships a sensible default, so the strip still scrolls if this
// never runs.

(function () {
    "use strict";

    var selectors = {
        self: '[data-cmp-is="marquee"]'
    };

    function init(element) {
        element.removeAttribute("data-cmp-is");

        var seconds = parseInt(element.getAttribute("data-cmp-marquee-duration"), 10);
        if (!isNaN(seconds) && seconds > 0) {
            element.style.setProperty("--cmp-marquee-duration", seconds + "s");
        }
    }

    function initAll(root) {
        [].slice.call(root.querySelectorAll(selectors.self)).forEach(init);
    }

    function onDocumentReady() {
        initAll(document);

        var MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
        if (!MutationObserver) {
            return;
        }
        new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                [].slice.call(mutation.addedNodes).forEach(function (added) {
                    if (added.querySelectorAll) {
                        initAll(added);
                    }
                });
            });
        }).observe(document.querySelector("body"), { subtree: true, childList: true });
    }

    if (document.readyState !== "loading") {
        onDocumentReady();
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }
}());
