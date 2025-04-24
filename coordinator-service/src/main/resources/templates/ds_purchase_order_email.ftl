<html>
<body>

<pre>
${"Drop Ship Order"?left_pad(47)}

<#if body.getBuyingAccount()??>
Bill To: ${body.getBuyingAccount().getMember().getName()}
</#if>

Account #: ${body.getBuyingAccount().getMember().getAccountNumber()}

<#list body.getParties() as party>
<#if party.getPartyType() == "ShipTo">
Ship To: ${party.getMember().getName()}
	<#if party.getMember().getAddress()??>
         ${party.getMember().getAddress()}
         ${party.getMember().getCity()}, ${party.getMember().getStateProv()} ${party.getMember().getPostalCode()}
	</#if>
</#if>
<#else>
</#list>
         
PO: ${body.getPurchaseOrder()}

Note:

<#list body.getParties() as party>
<#if party.getPartyType() == "Selling">
Vendor: ${party.getMember().getName()}
</#if>
<#else>
</#list>

<#assign total=0.0>
<#assign totalQty=0>
<#list body.getLineItems() as item>
<#assign total+=item.getPrice()>
<#assign totalQty+=item.getQuantity()>
${"0"?left_pad(2)}  -  Part: ${item.getCustomerPartNumber()?right_pad(42)} Qty: ${("" + item.getQuantity())?left_pad(4)}    $${(item.getPrice()?string["0.00"])?left_pad(10)}
<#else>
</#list>
${"Total Qty:"?left_pad(60)} ${("" + totalQty)?left_pad(4)}    $${(total?string["0.00"])?left_pad(10)}

${""?left_pad(80, "-")}
</pre>

</body>
</html>