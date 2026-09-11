/*
 * MJ Portfolio Grid — filtering.
 *
 * Everything is already in the DOM; this only hides and shows. That choice is what makes
 * the page work with the script disabled, keeps the whole index crawlable, and lets a
 * shared filtered URL render correctly for someone whose script never runs.
 *
 * Filter state lives in the query string so a filtered view can be sent to somebody.
 * replaceState rather than pushState: changing a facet is refining one view, not
 * navigating, and it should not take six presses of Back to leave the page.
 */
(function () {
    "use strict";

    var ROOT = "[data-cmp-grid]";
    var FACETS = ["sector", "stage", "year"];

    function readUrl() {
        var params = new URLSearchParams(window.location.search);
        var state = {};
        FACETS.forEach(function (facet) {
            state[facet] = params.get(facet) || "";
        });
        return state;
    }

    function writeUrl(state) {
        var params = new URLSearchParams(window.location.search);
        FACETS.forEach(function (facet) {
            if (state[facet]) {
                params.set(facet, state[facet]);
            } else {
                params.delete(facet);
            }
        });
        var query = params.toString();
        window.history.replaceState(null, "",
            window.location.pathname + (query ? "?" + query : "") + window.location.hash);
    }

    function setup(controls) {
        var section = controls.closest(".cmp-mjgrid");
        var list = section.querySelector("[data-cmp-grid-list]");
        var empty = section.querySelector("[data-cmp-grid-emptystate]");
        var count = section.querySelector("[data-cmp-grid-count]");
        var reset = section.querySelector("[data-cmp-grid-reset]");
        var items = Array.prototype.slice.call(list.querySelectorAll(".cmp-mjgrid__item"));
        var state = readUrl();

        function matches(item) {
            return FACETS.every(function (facet) {
                return !state[facet] || item.getAttribute("data-" + facet) === state[facet];
            });
        }

        function apply(writeHistory) {
            var shown = 0;
            items.forEach(function (item) {
                var keep = matches(item);
                // hidden rather than display:none in CSS: one attribute, and it keeps the
                // element out of the accessibility tree as well as out of the layout.
                item.hidden = !keep;
                if (keep) {
                    shown++;
                }
            });

            if (empty) {
                empty.hidden = shown !== 0;
            }
            if (count) {
                var total = items.length;
                count.textContent = shown === total
                    ? total + " companies"
                    : shown + " of " + total + " companies";
            }
            if (reset) {
                reset.hidden = !FACETS.some(function (facet) { return state[facet]; });
            }

            // Reflect the current state on the controls themselves, so a filtered URL
            // opens with the right pill pressed rather than with "All" lit.
            section.querySelectorAll("[data-cmp-grid-filter]").forEach(function (control) {
                var facet = control.getAttribute("data-cmp-grid-filter");
                if (control.tagName === "SELECT") {
                    control.value = state[facet];
                    return;
                }
                var active = (control.getAttribute("data-cmp-grid-value") || "") === state[facet];
                control.classList.toggle("is-active", active);
                control.setAttribute("aria-pressed", active ? "true" : "false");
            });

            if (writeHistory) {
                writeUrl(state);
            }
        }

        section.addEventListener("click", function (event) {
            var pill = event.target.closest("[data-cmp-grid-filter]");
            if (pill && pill.tagName === "BUTTON") {
                state[pill.getAttribute("data-cmp-grid-filter")] =
                    pill.getAttribute("data-cmp-grid-value") || "";
                apply(true);
                return;
            }
            if (event.target.closest("[data-cmp-grid-reset]")) {
                FACETS.forEach(function (facet) { state[facet] = ""; });
                apply(true);
            }
        });

        section.addEventListener("change", function (event) {
            var select = event.target.closest("select[data-cmp-grid-filter]");
            if (select) {
                state[select.getAttribute("data-cmp-grid-filter")] = select.value;
                apply(true);
            }
        });

        // First pass honours the URL but must not rewrite history on load.
        apply(false);
    }

    function init() {
        document.querySelectorAll(ROOT).forEach(setup);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
