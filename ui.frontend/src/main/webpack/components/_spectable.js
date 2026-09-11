// Technical Specification Table: unit switching and column sorting.
//
// Both unit systems are already in the DOM — the server decided which one starts hidden.
// This script only moves the `hidden` attribute between the two figures, so the table stays
// complete and correct if the script never runs. For the same reason the unit switch itself
// ships hidden and is revealed here: a control that cannot work should not be on the page.
//
// The visitor's choice is remembered for the session, because a page carrying several tables
// would otherwise ask them to switch each one, and the next programme page would forget again.

(function () {
    "use strict";

    var selectors = {
        self: '[data-cmp-is="spectable"]',
        units: '[data-cmp-hook-spectable="units"]',
        body: '[data-cmp-hook-spectable="body"]',
        unitButton: "[data-cmp-spectable-units]",
        figure: "[data-cmp-spectable-unit]",
        sortHeader: "[data-cmp-spectable-sort]"
    };

    var STORAGE_KEY = "aerovance.units";
    var METRIC = "metric";
    var IMPERIAL = "imperial";

    // Private browsing modes throw on access rather than returning null, and the preference
    // is a convenience — losing it must never take the table down with it.
    function readPreference() {
        try {
            var stored = window.sessionStorage.getItem(STORAGE_KEY);
            return stored === METRIC || stored === IMPERIAL ? stored : null;
        } catch (e) {
            return null;
        }
    }

    function writePreference(units) {
        try {
            window.sessionStorage.setItem(STORAGE_KEY, units);
        } catch (e) {
            // no persistence available; the switch still works for this page
        }
    }

    function applyUnits(table, units) {
        table.setAttribute("data-cmp-spectable-units", units);

        [].slice.call(table.querySelectorAll(selectors.figure)).forEach(function (figure) {
            figure.hidden = figure.getAttribute("data-cmp-spectable-unit") !== units;
        });

        [].slice.call(table.querySelectorAll(selectors.unitButton)).forEach(function (button) {
            button.setAttribute("aria-pressed",
                button.getAttribute("data-cmp-spectable-units") === units ? "true" : "false");
        });
    }

    function bindUnits(table) {
        var group = table.querySelector(selectors.units);
        if (!group) {
            return;
        }
        group.hidden = false;

        group.addEventListener("click", function (event) {
            var button = event.target.closest(selectors.unitButton);
            if (!button) {
                return;
            }
            var units = button.getAttribute("data-cmp-spectable-units");
            applyUnits(table, units);
            writePreference(units);

            // Other tables on the page follow, so a visitor switches once per page, not once
            // per table.
            [].slice.call(document.querySelectorAll(selectors.self)).forEach(function (other) {
                if (other !== table) {
                    applyUnits(other, units);
                }
            });
        });

        var preferred = readPreference();
        if (preferred) {
            applyUnits(table, preferred);
        }
    }

    // The visible text of a row's label or figure, used when a row has no numeric key.
    function textOf(row, column) {
        var cell = row.cells[column === "label" ? 0 : 1];
        return cell ? cell.textContent.trim().toLowerCase() : "";
    }

    function comparator(column, direction) {
        var sign = direction === "descending" ? -1 : 1;
        return function (a, b) {
            if (column === "value") {
                var keyA = parseFloat(a.getAttribute("data-cmp-spectable-key"));
                var keyB = parseFloat(b.getAttribute("data-cmp-spectable-key"));
                var numericA = !isNaN(keyA);
                var numericB = !isNaN(keyB);
                if (numericA && numericB) {
                    return (keyA - keyB) * sign;
                }
                // Rows whose value is not a number — "Classified", "N/A" — collect at the
                // bottom in both directions rather than drifting to the top on a reverse sort.
                if (numericA !== numericB) {
                    return numericA ? -1 : 1;
                }
            }
            return textOf(a, column).localeCompare(textOf(b, column)) * sign;
        };
    }

    function bindSorting(table) {
        var headers = [].slice.call(table.querySelectorAll(selectors.sortHeader));
        var body = table.querySelector(selectors.body);
        if (!body || !headers.length) {
            return;
        }

        // The authored order is meaningful, so a third click restores it rather than
        // cycling endlessly between the two sorted directions.
        var original = [].slice.call(body.rows);

        headers.forEach(function (header) {
            var button = header.querySelector("button");
            if (!button) {
                // Sorting is off for this component; the header is a plain label.
                return;
            }
            button.addEventListener("click", function () {
                var current = header.getAttribute("aria-sort");
                var next = current === "ascending" ? "descending"
                    : current === "descending" ? "none"
                        : "ascending";

                headers.forEach(function (other) {
                    other.setAttribute("aria-sort", other === header ? next : "none");
                });

                var rows = next === "none"
                    ? original.slice()
                    : original.slice().sort(comparator(header.getAttribute("data-cmp-spectable-sort"), next));

                var fragment = document.createDocumentFragment();
                rows.forEach(function (row) {
                    fragment.appendChild(row);
                });
                body.appendChild(fragment);
            });
        });
    }

    function SpecTable(table) {
        // Prevents a second initialization when the mutation observer sees it again.
        table.removeAttribute("data-cmp-is");
        bindUnits(table);
        bindSorting(table);
    }

    function initAll(root) {
        [].slice.call(root.querySelectorAll(selectors.self)).forEach(SpecTable);
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
