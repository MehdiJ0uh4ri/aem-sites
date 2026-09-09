// Client-side behaviour for the Stats Bar component.
//
// Counts each value up from zero the first time the component scrolls into view. The numeric
// part and its decoration are supplied by the Sling Model as data attributes, so the script
// never has to parse the rendered text. Values without a number, and visitors who asked for
// reduced motion, keep the server-rendered markup untouched.

(function () {
    "use strict";

    var selectors = {
        self: '[data-cmp-is="statsbar"]',
        value: '[data-cmp-hook-statsbar="value"]'
    };

    var DURATION_MS = 1200;

    function prefersReducedMotion() {
        return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }

    function formatNumber(value, decimals) {
        return value.toLocaleString(undefined, {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        });
    }

    function countUp(element) {
        var target = parseFloat(element.getAttribute("data-cmp-statsbar-number"));
        if (isNaN(target)) {
            return;
        }

        var finalText = element.textContent;
        var prefix = element.getAttribute("data-cmp-statsbar-prefix") || "";
        var suffix = element.getAttribute("data-cmp-statsbar-suffix") || "";
        // Animate with the same precision the author entered, so 1.2 does not render as 1.
        var decimals = (String(target).split(".")[1] || "").length;
        var start = null;

        function step(timestamp) {
            if (start === null) {
                start = timestamp;
            }
            var progress = Math.min((timestamp - start) / DURATION_MS, 1);
            // Ease-out cubic, so the number decelerates towards its final value.
            var eased = 1 - Math.pow(1 - progress, 3);

            if (progress < 1) {
                element.textContent = prefix + formatNumber(target * eased, decimals) + suffix;
                window.requestAnimationFrame(step);
            } else {
                // Restore the authored string verbatim, keeping the original formatting.
                element.textContent = finalText;
            }
        }

        element.textContent = prefix + formatNumber(0, decimals) + suffix;
        window.requestAnimationFrame(step);
    }

    function animate(element) {
        var values = [].slice.call(element.querySelectorAll(selectors.value));
        values.forEach(countUp);
    }

    function StatsBar(config) {
        var element = config.element;

        // Prevents a second initialization when the observer sees the same element again.
        element.removeAttribute("data-cmp-is");

        if (prefersReducedMotion() || !window.requestAnimationFrame) {
            return;
        }

        if (!window.IntersectionObserver) {
            animate(element);
            return;
        }

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    observer.unobserve(entry.target);
                    animate(entry.target);
                }
            });
        }, { threshold: 0.25 });

        observer.observe(element);
    }

    function initAll(root) {
        var elements = [].slice.call(root.querySelectorAll(selectors.self));
        elements.forEach(function (element) {
            new StatsBar({ element: element });
        });
    }

    function onDocumentReady() {
        initAll(document);

        // Re-initializes the component after an author drops it on the page or edits its dialog.
        var MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
        if (!MutationObserver) {
            return;
        }

        var observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                [].slice.call(mutation.addedNodes).forEach(function (addedNode) {
                    if (addedNode.querySelectorAll) {
                        initAll(addedNode);
                    }
                });
            });
        });

        observer.observe(document.querySelector("body"), {
            subtree: true,
            childList: true
        });
    }

    if (document.readyState !== "loading") {
        onDocumentReady();
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }
}());
