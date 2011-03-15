<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" >
<title><%=pageTitle %></title>
<style type="text/css">
*  {    font-family: Arial;
        font-size: 10pt;
    }
th {    vertical-align: top;
        text-align: center;
        background: #666;
        color:#fff;
        padding: 5px;
        border-width: 1px;
        border-style: solid;
        border-color: black;
    }
td {    vertical-align: top;
        text-align: center;
        white-space: nowrap;
        padding: 5px;
        border-width: 1px;
        border-style: solid;
        border-color: gray;
        background:#fff;
    }

th.header { 
    background-image: url('../images/bg.gif');     
	background-color: #cccccc; 
	color:black;
    cursor: pointer; 
    font-weight: bold; 
    background-repeat: no-repeat; 
    background-position: center left; 
    padding-left: 20px; 
    border-right: 1px solid #dad9c7; 
    margin-left: -1px; 
}


th.header.headerSortUp {
	background-image: url('../images/asc.gif');
	background-color: #6d7cdb; 	
}

th.header.headerSortDown {
	background-image: url('../images/desc.gif');
	background-color: #6d7cdb; 	
}

.sorting_asc {
	background: url('../images/sort_asc.jpg') no-repeat center right;
}

.sorting_desc {
	background: url('../images/sort_desc.jpg') no-repeat center right;
}

.sorting {
	background: url('../images/sort_both.jpg') no-repeat center right;
}


</style>
</head>
<body>
