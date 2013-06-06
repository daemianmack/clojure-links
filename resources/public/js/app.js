// from
// http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript/105074#105074
// not a true guid but good enough for now
function s4() {
  return Math.floor((1 + Math.random()) * 0x10000)
             .toString(16)
             .substring(1);
};

function guid() {
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
         s4() + '-' + s4() + s4() + s4();
};

function store_history() {
  if (!(window.history && window.history.pushState)) {
    return;
  }
  window.history.replaceState({"links": $("#links").html()}, null, '');
};

function apply_history(links) {
  $("#links").html(links);
};

function initEventSource(clientId) {
  var es = new EventSource('/updates?id='+clientId);
  es.onmessage = function(e) {
    // Assume any query string means user has requested a non-first
    // page. Will need to get smarter if providing SSE new-link
    // display for non-first pages.
    // Ignores possibility someone manually requested /?p=1.
    if (!location.search) {
      // Could be smarter re: placing the item below any on-page items
      // with newer created_at timestamps, handling condition where
      // two links in title-fetching flight race and the later one
      // wins and comes in out of order via SSE.
      $("#links tr:first").before($(e.data).fadeIn(1400));
    };
  };
  es.onerror = function(e) {
    console.log("error!");
    console.log(e);
  };
  return es;
};

var clientId = guid();
var es = initEventSource(clientId);

$(function() {
  window.addEventListener("popstate", function(e) {
    // guard against initial page load in chrome
    if (e.state) {
      apply_history(e.state.links);
    }
  });

  window.onbeforeunload = function(e) {
    store_history();
  };
});
