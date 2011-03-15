group html;

agg(agg, ident) ::= <<
<tr>
  <td align="left" nowrap> $ident$ $agg.path$ </td>
  <td> <a href="/xn/rest/1.0/event/dictionary/$agg.aggregator.inputEvent$"> $agg.aggregator.inputEvent$ </a> [<a href="/xn/rest/1.0/event/instream/$agg.aggregator.inputEvent$"> stream </a>] </td>
  <td> <a href="/xn/rest/1.0/event/dictionary/$agg.aggregator.outputEvent$"> $agg.aggregator.outputEvent$ </a> [<a href="/xn/rest/1.0/event/stream/$agg.path$"> stream </a>] </td>
  <td> <pre>$agg.aggregator.statement$ </pre></td>
</tr>
>>

eventDef(def) ::= <<
<p> $def.name$ </p>
<p> Event attributes : </p>
<table>
  <tr>
     <th> Attribute </th>
     <th> Type </th>
  <tr>

  $def.properties.entrySet:{entry|
    <tr>
       <td> $entry.key$ </td>
       <td> $entry.value$ </td>
    </tr>
  } ; separator = "\n" $

</table>
>>