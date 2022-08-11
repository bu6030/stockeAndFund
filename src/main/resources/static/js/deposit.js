var pageSize = 15;

function getData() {
    $.ajax({
        url:"/deposit",
        type:"get",
        data :{
        },
        dataType:'json',
        contentType: 'application/x-www-form-urlencoded',
        success: function (data){
            var result = data.value;
            var str = getTableHtml(result);
            $("#nr").html(str);
        },
        error: function(XMLHttpRequest, textStatus, errorThrown) {
            console.log(XMLHttpRequest.status);
            console.log(XMLHttpRequest.readyState);
            console.log(textStatus);
        }
    });

    lay('#version').html('-v'+ laydate.v);
}

function getTableHtml(result){
    var str = "";
    var totalTotalDayIncome = 0;
    var totalFundDayIncome = 0;
    var totalStockDayIncome = 0;
    for(var k in result) {
        totalFundDayIncome = totalFundDayIncome + parseFloat(result[k].fundDayIncome);
        totalStockDayIncome = totalStockDayIncome + parseFloat(result[k].stockDayIncome);
        totalTotalDayIncome = totalTotalDayIncome + parseFloat(result[k].totalDayIncome);
        str += "<tr><td>" + result[k].date
            + "</td><td>" + parseFloat(result[k].fundDayIncome).toFixed(2)
            + "</td><td>" + parseFloat(result[k].stockDayIncome).toFixed(2)
            + "</td><td>" + parseFloat(result[k].totalDayIncome).toFixed(2)
            + "</td><td>" + parseFloat(result[k].fundMarketValue).toFixed(2)
            + "</td><td>" + parseFloat(result[k].stockMarketValue).toFixed(2)
            + "</td><td>" + parseFloat(result[k].totalMarketValue).toFixed(2)
            +"</td></tr>";

    }
    str += "<tr><td>合计</td><td>" + totalFundDayIncome.toFixed(2) + "</td><td>" + totalStockDayIncome.toFixed(2) + "</td><td>" + totalTotalDayIncome.toFixed(2)
        +"</td></tr>";
    return str;
}