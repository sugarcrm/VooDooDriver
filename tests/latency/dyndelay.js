/*
 * dyndelay.js --
 *
 *      Code to dynamically fetch an element with a configurable delay.  This
 *      assumes that index.js has been loaded.
 */


function clicked() {
  var time = document.getElementById('time').value;
  get('dyndelay.pl', ['time=' + time], function(t){ set('target', t); });
  return false;
}
