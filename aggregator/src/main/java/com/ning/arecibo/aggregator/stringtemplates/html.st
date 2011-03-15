group html ;

htmlOpen(refresh, header, msg) ::= <<
<html>
   <head>
      <meta http-equiv="refresh" content="$if(refresh)$$refresh$$else$15$endif$" >
   </head>
   <body>
      <h2> $header$ </h2>
      <p> $msg$ </p>
      <p> <a href="/"> home </a> </p>
>>

aggJS() ::= <<

<script language="javascript">

function deleteAgg(name)
{
    if ( confirm('deleting ' + name + '\n Are you sure ?') )
    {
		var request =  new XMLHttpRequest();
		request.open("DELETE", "/xn/rest/1.0/event/aggregator/"+name, true);
		request.send(null);
		setTimeout("window.self.location = window.self.location", 500); 
	}
}

</script>


>>

htmlClose() ::= <<
   </body>
</html>
>>

formOpen(name) ::= <<
<form name="$name$" method="GET" target="#" onSubmit="null">
>>

formClose() ::= <<
</form>
>>

button(label) ::= <<
<input type="submit" value="$label$">
>>

deleteButton(label, name) ::= <<
<button name="$name$" onclick="deleteAgg('$name$')">$label$</button>
>>


tableHeader(headers) ::= <<
  <tr>
    $headers:{h|<th> $h$ </th>}; separator = "\n"$
  </tr>
>>

tableOpen() ::= <<
<table border=1>
>>

tableClose() ::= <<
</table>
>>

eventDef(def, properties) ::= <<
<table border=1 cellpadding=2>
  <tr> <th colspan="2" align="center"> Event Type </th> </tr>
  <tr> <td colspan="2" align="center"> $def.eventType$ </td> </tr>
  <tr>
     <th> Attribute </th>
     <th> Type </th>
  <tr>

  $properties:{entry|
    <tr>
       <td> $entry.key$ </td>
       <td> $entry.value.name$ </td>
    </tr>
  } ; separator = "\n" $

</table>
>>

test(abc) ::= << 
 $if(abc)$ yes $else$ no $endif$
>>