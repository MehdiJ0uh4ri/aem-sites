// Shared scroll-reveal for the marketing sections.
//
// Any element carrying data-cmp-reveal fades and lifts into place the first time it
// enters the viewport. The CSS that hides elements is gated behind the
// `cmp-reveal-ready` class this script puts on <html>, so if the script never runs
// nothing is hidden and the page degrades to a plain static layout.

(function () {
    "use strict";

    var SELECTOR = "[data-cmp-reveal]";
    var READY_CLASS = "cmp-reveal-ready";
    var VISIBLE_CLASS = "is-revealed";

    var observer = null;

    function markReady() {
        // Applied as early as possible to avoid a flash of fully-rendered content
        // before the first paint of the hidden state.
        document.documentElement.classList.add(READY_CLASS);
    }

    function reveal(element) {
        element.classList.add(VISIBLE_CLASS);
    }

    function getObserver() {
        if (observer) {
            return observer;
        }
        observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    observer.unobserve(entry.target);
                    reveal(entry.target);
                }
            });
        }, { threshold: 0.12, rootMargin: "0px 0px -8% 0px" });
        return observer;
    }

    function observe(root) {
        var elements = [].slice.call(root.querySelectorAll(SELECTOR));
        if (!elements.length) {
            return;
        }
        markReady();

        if (!window.IntersectionObserver) {
            elements.forEach(reveal);
            return;
        }
        var io = getObserver();
        elements.forEach(function (element) {
            if (!element.classList.contains(VISIBLE_CLASS)) {
                io.observe(element);
            }
        });
    }

    function onDocumentReady() {
        observe(document);

        // Re-arms the effect when an author drops or edits a component.
        var MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
        if (!MutationObserver) {
            return;
        }
        new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                [].slice.call(mutation.addedNodes).forEach(function (added) {
                    if (added.querySelectorAll) {
                        observe(added);
                    }
                });
            });
        }).observe(document.querySelector("body"), { subtree: true, childList: true });
    }

    if (document.querySelector(SELECTOR)) {
        markReady();
    }

    if (document.readyState !== "loading") {
        onDocumentReady();
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }
}());
