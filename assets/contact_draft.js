var tehResult = ''
var inputs = document.getElementsByTagName('input');

for(var i=0; i<inputs.length; i++) {
    if(inputs[i].getAttribute('type')=='radio') {
        if(inputs[i].checked)   tehResult = tehResult + '|' + inputs[i].id;
    } else
        if(inputs[i].getAttribute('type')!='hidden' && inputs[i].getAttribute('type')!='submit') {
            tehResult = tehResult + '|' + inputs[i].id + '=' + inputs[i].value;
        }
}

inputs = document.getElementsByTagName('textarea');
for(var i=0; i<inputs.length; i++) {
    if(inputs[i].id!='comment')
        tehResult = tehResult + '|' + inputs[i].id + '=' + inputs[i].value;
}

inputs = document.getElementsByTagName('select');
for(var i=0; i<inputs.length; i++) {
    tehResult = tehResult + '|' + inputs[i].id + '=' + inputs[i].options[inputs[i].selectedIndex].value;
}