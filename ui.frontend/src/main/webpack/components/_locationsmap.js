// Global Locations Map: business-unit filtering.
//
// The whole dataset is already in the page, so filtering hides rows rather than fetching
// anything. The filter controls ship hidden and are revealed here, so a visitor without
// JavaScript sees every facility and no controls that do nothing — which for a page whose
// job is to give people addresses is the right fallback.

(function () {
    "use strict";

    var selectors = {
        self: '[data-cmp-is="locationsmap"]',
        filters: '[data-cmp-hook-locationsmap="filters"]',
        empty: '[data-cmp-hook-locationsmap="empty"]',
        button: "[data-cmp-locationsmap-filter]",
        unit: "[data-cmp-locationsmap-unit]"
    };

    var ALL = "all";

    function apply(root, unit) {
        var items = [].slice.call(root.querySelectorAll(selectors.unit));
        var visible = 0;

        items.forEach(function (item) {
            var matches = unit === ALL || item.getAttribute("data-cmp-locationsmap-unit") === unit;
            item.hidden = !matches;
            // Pins and list entries share the attribute, so both are filtered by the same
            // pass; only list entries are counted, or every facility would count twice.
            if (matches && item.tagName === "LI") {
                visible++;
            }
        });

        [].slice.call(root.querySelectorAll(selectors.button)).forEach(function (button) {
            button.setAttribute("aria-pressed",
                button.getAttribute("data-cmp-locationsmap-filter") === unit ? "true" : "false");
        });

        var empty = root.querySelector(selectors.empty);
        if (empty) {
            empty.hidden = visible > 0;
        }
    }

    function LocationsMap(root) {
        // Prevents a second initialization when the mutation observer sees it again.
        root.removeAttribute("data-cmp-is");

        var filters = root.querySelector(selectors.filters);
        if (!filters) {
            return;
        }
        filters.hidden = false;

        filters.addEventListener("click", function (event) {
            var button = event.target.closest(selectors.button);
            if (button) {
                apply(root, button.getAttribute("data-cmp-locationsmap-filter"));
            }
        });
    }

    function initAll(root) {
        [].slice.call(root.querySelectorAll(selectors.self)).forEach(LocationsMap);
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
