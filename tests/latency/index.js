/*
 * index.js --
 *
 *      Functions that interact with index.html.  
 */


/*
 * set --
 *
 *      Set the specified element's innerHTML to the specified text.
 */

function set(el, txt) {
  document.getElementById(el).innerHTML = txt;
}


/*
 * error --
 *
 *      Format an error message.
 */

function error(msg) {
  return '<span class="error">' + msg + '</span>';
}


/*
 * check_response --
 *
 *      Check the response for errors, and post either the returned text
 *      or the error message.
 */

function check_response(r, fcn) {
  if (r.readyState == 4) {
    if (r.status == 200) {
      fcn(r.responseText);
    } else {
      fcn(error('Error: ' + r.status));
    }
  }
}


/*
 * get --
 *
 *      Asynchronously get text from the server.
 */

function get(uri, params, fcn) {
  var r = false;

  if (window.XMLHttpRequest) {
    r = new XMLHttpRequest();
  } else if (window.ActiveXObject) {
    // IE
    try {
      r = new ActiveXObject("Msxml2.XMLHTTP");
    } catch (e) {
      try {
        r = new ActiveXObject("Microsoft.XMLHTTP");
      } catch (e) {}
    }
  }

  if (!r) {
    fcn(error("Unable to asynchronously load."));
    return;
  }

  if (params.length > 0) {
    uri += '?' + params.join('&');
  }

  r.onreadystatechange = function() { check_response(r, fcn); };
  r.open ('GET', uri, true);
  r.send (null);
}


/*
 * loadtestsel --
 *
 *      Load the test selectors and convert into a form with checkboxes.
 *      Since this is the first function to be called (it's called by
 *      the BODY's onload), it also verifies that the latency.js
 *      functionality is available.
 */

function loadtestsel() {
  get('latency.pl', ['load=true'], function (t) { set('testsel', t); });
}


/*
 * runtests --
 *
 *      Run the selected tests.
 */

function runtests() {
  var test = false;
  var radios = document.getElementsByName('selected_test');

  for (var k = 0; k < radios.length; k++) {
    if (radios[k].checked) {
      test = radios[k].value;
    }
  }

  if (test) {
    get('latency.pl', [test + '=true'], function(t) { set('content', t); });
    set('content', '<p>Running test "' + test + '"...</p>');
  } else {
    set('content', error('Select a test to run.'));
  }

  return false;
}
