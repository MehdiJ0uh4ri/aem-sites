// Progressive enhancement for the Slides track.
//
// The track scrolls natively, so this only adds arrow buttons and keeps their disabled
// state in sync. The controls ship with a `hidden` attribute and are only revealed here,
// which means a visitor without JavaScript never sees buttons that would not work.

(function () {
    "use strict";

    var selectors = {
        self: '[data-cmp-is="slides"]',
        track: '[data-cmp-hook-slides="track"]',
        controls: '[data-cmp-hook-slides="controls"]',
        prev: '[data-cmp-hook-slides="prev"]',
        next: '[data-cmp-hook-slides="next"]'
    };

    function prefersReducedMotion() {
        return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }

    function init(element) {
        element.removeAttribute("data-cmp-is");

        var track = element.querySelector(selectors.track);
        var controls = element.querySelector(selectors.controls);
        var prev = element.querySelector(selectors.prev);
        var next = element.querySelector(selectors.next);
        if (!track || !controls || !prev || !next) {
            return;
        }

        // The viewport is the scrolling element, not the track itself.
        var viewport = track.parentElement;
        var slides = track.children;
        if (slides.length < 2) {
            return;
        }

        controls.hidden = false;

        function step() {
            // Advance by one slide plus the gap between them.
            var first = slides[0];
            var second = slides[1];
            return second ? second.offsetLeft - first.offsetLeft : first.offsetWidth;
        }

        function scrollBy(direction) {
            viewport.scrollBy({
                left: direction * step(),
                behavior: prefersReducedMotion() ? "auto" : "smooth"
            });
        }

        function syncDisabled() {
            // A one-pixel tolerance: fractional scroll positions never land exactly.
            var maxScroll = viewport.scrollWidth - viewport.clientWidth;
            prev.disabled = viewport.scrollLeft <= 1;
            next.disabled = viewport.scrollLeft >= maxScroll - 1;
        }

        prev.addEventListener("click", function () {
            scrollBy(-1);
        });
        next.addEventListener("click", function () {
            scrollBy(1);
        });
        viewport.addEventListener("scroll", syncDisabled, { passive: true });
        window.addEventListener("resize", syncDisabled);

        syncDisabled();
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
