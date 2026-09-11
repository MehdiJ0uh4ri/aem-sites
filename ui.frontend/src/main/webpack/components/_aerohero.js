// Background video for the Aerospace Hero.
//
// The markup ships the video with preload="none" and no autoplay attribute, so a visitor
// who never reaches the hero — or who asked for reduced motion — pays nothing for it and
// keeps the poster still. This script is what decides to fetch and play, and it pauses
// again the moment the section leaves the viewport so a long page does not decode video
// off-screen.

(function () {
    "use strict";

    var SELECTOR = '[data-cmp-is="aerohero-video"]';

    function prefersReducedMotion() {
        return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }

    function play(video) {
        if (video.preload === "none") {
            video.preload = "auto";
            video.load();
        }
        // Autoplay is refused on some devices (low power mode, data saver). The poster
        // stays on screen in that case, which is the intended fallback, so the rejection
        // is swallowed rather than reported.
        var started = video.play();
        if (started && typeof started.catch === "function") {
            started.catch(function () { /* poster remains */ });
        }
    }

    function AeroHeroVideo(video) {
        // Prevents a second initialization when the mutation observer sees it again.
        video.removeAttribute("data-cmp-is");

        if (prefersReducedMotion()) {
            return;
        }

        if (!window.IntersectionObserver) {
            play(video);
            return;
        }

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    play(entry.target);
                } else {
                    entry.target.pause();
                }
            });
        }, { threshold: 0.15 });

        observer.observe(video);
    }

    function initAll(root) {
        [].slice.call(root.querySelectorAll(SELECTOR)).forEach(AeroHeroVideo);
    }

    function onDocumentReady() {
        initAll(document);

        // Re-initializes after an author drops the component or edits its dialog.
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
