<#assign page_title="Accounts">

<#include "/template/layouts/global_header.ftl">

<a href="/spendhawk/accounts/manage">Manage Accounts</a>

<h2>Accounts in database:</h2>
<table border='1'>
    <tr>
        <th>Account Name</th>
        <th>Account Balance</th>
    </tr>

<#list accounts as account>
    <tr>
        <td>
            <a href="/spendhawk/account?id=${account.id}">
                ${account.name}
            </a>
        </td>
        <td>${account.balance}</td>
    </tr>
</#list>

</table>

<#include "/template/layouts/global_footer.ftl">