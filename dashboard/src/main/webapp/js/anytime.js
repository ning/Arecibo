/*****************************************************************************
 *  FILE:  anytime.js - The Any+Time(TM) JavaScript Library (source)
 *
 *  VERSION: 3.991
 *
 *  Copyright 2008-2010 Andrew M. Andrews III (www.AMA3.com). Some Rights 
 *  Reserved. This work licensed under the Creative Commons Attribution-
 *  Noncommercial-Share Alike 3.0 Unported License except in jurisdicitons
 *  for which the license has been ported by Creative Commons International,
 *  where the work is licensed under the applicable ported license instead.
 *  For a copy of the unported license, visit
 *  http://creativecommons.org/licenses/by-nc-sa/3.0/
 *  or send a letter to Creative Commons, 171 Second Street, Suite 300,
 *  San Francisco, California, 94105, USA.  For ported versions of the
 *  license, visit http://creativecommons.org/international/
 *
 *  Alternative licensing arrangements may be made by contacting the
 *  author at http://www.AMA3.com/contact/
 *
 *  The Any+Time JavaScript Library provides the following ECMAScript
 *  functionality:
 *
 *    AnyTime.Converter
 *      Converts Dates to/from Strings, allowing a wide range of formats
 *      closely matching those provided by the MySQL DATE_FORMAT() function,
 *      with some noteworthy enhancements.
 *
 *    AnyTime.pad()
 *      Pads a value with a specific number of leading zeroes.
 *      
 *    AnyTime.widget()
 *      Attaches a calendar widget to a text field for selecting date/time
 *      values with fewer mouse movements than most similar widgets.  Any
 *      format supported by AnyTime.Converter can be used for the text field.
 *      If JavaScript is disabled, the text field remains editable without
 *      any of the widget features.
 *
 *  IMPORTANT NOTICE:  This code depends upon the jQuery JavaScript Library
 *  (www.jquery.com), currently version 1.3.2.
 *
 *  The AnyTime code and styles in anytime.css have been tested (but not
 *  extensively) on Windows Vista in Internet Explorer 8.0, Firefox 3.0, Opera
 *  10.10 and Safari 4.0.  Minor variations in IE6+7 are to be expected, due
 *  to their broken box model. Please report any other problems to the author
 *  (URL above).
 *
 *  Any+Time is a trademark of Andrew M. Andrews III.
 *  Thanks to Chu for help with a setMonth() issue!
 ****************************************************************************/

var AnyTime = (function()
{
	// private members

  	var __acb = '.AtwCurrentBtn';
	var __daysIn = [ 31,28,31,30,31,30,31,31,30,31,30,31 ];
	var __iframe = null;
	var __initialized = false;
	var __msie6 = ( navigator.userAgent.indexOf('MSIE 6') > 0 ); 
	var __msie7 = ( navigator.userAgent.indexOf('MSIE 7') > 0 ); 
  	var __widgets = [];

	
  	//	Add special methods to jQuery to compute the height and width
	//	of widget components differently for Internet Explorer 6.x
	//  This prevents the widgets from being too tall and wide.
  	
  	jQuery.prototype.AtwHeight = function(inclusive)
  	{
  		return ( __msie6 ?
  					Number(this.css('height').replace(/[^0-9]/g,'')) :
  					this.outerHeight(inclusive) );
  	};

  	jQuery.prototype.AtwWidth = function(inclusive)
  	{
  		return ( __msie6 ?
  					(1+Number(this.css('width').replace(/[^0-9]/g,''))) :
  					this.outerWidth(inclusive) );
  	};

  	$(document).ready( 
  		function()
		{
			//  Ping the server for statistical purposes (remove if offended).

            /*
            ** removing, jbr
            **
  			if ( window.location.hostname.length && ( window.location.hostname != 'www.ama3.com' ) )
  				$(document.body).append('<img src="http://www.ama3.com/anytime/ping/" width="0" height="0" />');
  	        */
			
			//  IE6 doesn't float popups over <select> elements unless an
			//	<iframe> is inserted between them!  The <iframe> is added to
			//	the page *before* the popups are moved, so they will appear
			//  after the <iframe>.
			
			if ( __msie6 )
			{
				__iframe = $('<iframe frameborder="0" scrolling="no"></iframe>');
				__iframe.src = "javascript:'<html></html>';";
				$(__iframe).css( {
					display: 'block',
					height: '1px',
					left: '0',
					top: '0',
					width: '1px',
					zIndex: 0
					} );
				$(document.body).append(__iframe);
			}
			
			//  Move popup windows to the end of the page.  This allows them to
			//  overcome XHTML restrictions on <table> placement enforced by MSIE.
			
			for ( var id in __widgets )
			  __widgets[id].onReady();
			
			__initialized = true;
		
		} ); // document.ready
  	
  	return {

//=============================================================================
//  AnyTime.Converter
//
//  This object converts between Date objects and Strings.
//
//  To use AnyTime.Converter, simply create an instance for a format string,
//  and then (repeatedly) invoke the format() and/or parse() methods to
//  perform the conversions.  For example:
//
//    var converter = new AnyTime.Converter({format:'%Y-%m-%d'})
//    var datetime = converter.parse('1967-07-30') // July 30, 1967 @ 00:00
//    alert( converter.format(datetime) ); // outputs: 1967-07-30
//
//  Constructor parameter:
//
//  options - an object of optional parameters that override default behaviors.
//    The supported options are:
//
//    baseYear - the number to add to two-digit years if the %y format
//      specifier is used.  By default, AnyTime.Converter follows the
//      MySQL assumption that two-digit years are in the range 1970 to 2069
//      (see http://dev.mysql.com/doc/refman/5.1/en/y2k-issues.html).
//      The most common alternatives for baseYear are 1900 and 2000.
//
//    dayAbbreviations - an array of seven strings, indexed 0-6, to be used
//      as ABBREVIATED day names.  If not specified, the following are used:
//      ['Sun','Mon','Tue','Wed','Thu','Fri','Sat']
//      Note that if the firstDOW option is passed to AnyTime.Widget() (see
//      AnyTime.Widget()), this array should nonetheless begin with the 
//      desired abbreviation for Sunday.
//
//    dayNames - an array of seven strings, indexed 0-6, to be used as
//      day names.  If not specified, the following are used: ['Sunday',
//        'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday']
//      Note that if the firstDOW option is passed to AnyTime.Widget() (see
//      AnyTime.Widget()), this array should nonetheless begin with the
//      desired name for Sunday.
//
//    eraAbbreviations - an array of two strings, indexed 0-1, to be used
//      as ABBREVIATED era names.  Item #0 is the abbreviation for "Before
//      Common Era" (years before 0001, sometimes represented as negative
//      years or "B.C"), while item #1 is the abbreviation for "Common Era"
//      (years from 0001 to present, usually represented as unsigned years
//      or years "A.D.").  If not specified, the following are used:
//      ['BCE','CE']
//
//    format - a string specifying the pattern of strings involved in the
//      conversion.  The parse() method can take a string in this format and
//      convert it to a Date, and the format() method can take a Date object
//      and convert it to a string matching the format.
//
//      Fields in the format string must match those for the DATE_FORMAT()
//      function in MySQL, as defined here:
//      http://tinyurl.com/bwd45#function_date-format
//
//      IMPORTANT:  Some MySQL specifiers are not supported (especially
//      those involving day-of-the-year, week-of-the-year) or approximated.
//      See the code for exact behavior.
//
//      In addition to the MySQL format specifiers, the following custom
//      specifiers are also supported:
//
//        %B - If the year is before 0001, then the "Before Common Era"
//          abbreviation (usually BCE or the obsolete BC) will go here.
//
//        %C - If the year is 0001 or later, then the "Common Era"
//          abbreviation (usually CE or the obsolete AD) will go here.
//
//        %E - If the year is before 0001, then the "Before Common Era"
//          abbreviation (usually BCE or the obsolete BC) will go here.
//          Otherwise, the "Common Era" abbreviation (usually CE or the
//          obsolete AD) will go here.
//
//        %Z - The current four-digit year, without any sign.  This is
//          commonly used with years that might be before (or after) 0001,
//          when the %E (or %B and %C) specifier is used instead of a sign.
//          For example, 45 BCE is represented "0045".  By comparison, in
//          the "%Y" format, 45 BCE is represented "-0045".
//
//        %z - The current year, without any sign, using only the necessary
//          number of digits.  This if the year is commonly used with years
//          that might be before (or after) 0001, when the %E (or %B and %C)
//          specifier is used instead of a sign.  For example, the year
//          45 BCE is represented as "45", and the year 312 CE as "312".
//
//    monthAbbreviations - an array of twelve strings, indexed 0-6, to be
//      used as ABBREVIATED month names.  If not specified, the following
//      are used: ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep',
//        'Oct','Nov','Dec']
//
//    monthNames - an array of twelve strings, indexed 0-6, to be used as
//      month names.  If not specified, the following are used:
//      ['January','February','March','April','May','June','July',
//        'August','September','October','November','December']
//=============================================================================

Converter: function(options)
{
  	// private members

  	var _flen = 0;
	var _longDay = 9;
	var _longMon = 9;
	var _shortDay = 6;
	var _shortMon = 3;

	// public members
  
	this.fmt = '%Y-%m-%d %T';
	this.dAbbr = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
	this.dNames = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
	this.dNums = [];
	this.eAbbr = ['BCE','CE'];
	this.mAbbr = [ 'Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec' ];
	this.mNames = [ 'January','February','March','April','May','June','July','August','September','October','November','December' ];
	this.mNums = [];
	this.baseYear = null;
	
	//-------------------------------------------------------------------------
	//  AnyTime.Converter.dAt() returns true if the character in str at pos
	//  is a digit.
	//-------------------------------------------------------------------------
	
	this.dAt = function( str, pos )
	{
	    return ( (str.charCodeAt(pos)>='0'.charCodeAt(0)) &&
	            (str.charCodeAt(pos)<='9'.charCodeAt(0)) );
	};
	
	//-------------------------------------------------------------------------
	//  AnyTime.Converter.format() returns a String containing the value
	//  of a specified Date object, using the format string passed to
	//  AnyTime.Converter().
	//
	//  Method parameter:
	//
	//    date - the Date object to be converted
	//-------------------------------------------------------------------------
	
	this.format = function( date )
	{
	    var t;
	    var str = '';
	    for ( var f = 0 ; f < _flen ; f++ )
	    {
	      if ( this.fmt.charAt(f) != '%' )
	        str += this.fmt.charAt(f);
	      else
	      {
	        switch ( this.fmt.charAt(f+1) )
	        {
	          case 'a': // Abbreviated weekday name (Sun..Sat)
	            str += this.dAbbr[ date.getDay() ];
	            break;
	          case 'B': // BCE string (eAbbr[0], usually BCE or BC, only if appropriate) (NON-MYSQL)
	            if ( date.getFullYear() < 0 )
	              str += this.eAbbr[0];
	            break;
	          case 'b': // Abbreviated month name (Jan..Dec)
	            str += this.mAbbr[ date.getMonth() ];
	            break;
	          case 'C': // CE string (eAbbr[1], usually CE or AD, only if appropriate) (NON-MYSQL)
	            if ( date.getFullYear() > 0 )
	              str += this.eAbbr[1];
	            break;
	          case 'c': // Month, numeric (0..12)
	            str += date.getMonth()+1;
	            break;
	          case 'd': // Day of the month, numeric (00..31)
	            t = date.getDate();
	            if ( t < 10 ) str += '0';
	            str += String(t);
	            break;
	          case 'D': // Day of the month with English suffix (0th, 1st,...)
	            t = String(date.getDate());
	            str += t;
	            if ( ( t.length == 2 ) && ( t.charAt(0) == '1' ) )
	              str += 'th';
	            else
	            {
	              switch ( t.charAt( t.length-1 ) )
	              {
	                case '1': str += 'st'; break;
	                case '2': str += 'nd'; break;
	                case '3': str += 'rd'; break;
	                default: str += 'th'; break;
	              }
	            }
	            break;
	          case 'E': // era string (from eAbbr[], BCE, CE, BC or AD) (NON-MYSQL)
	            str += this.eAbbr[ (date.getFullYear()<0) ? 0 : 1 ];
	            break;
	          case 'e': // Day of the month, numeric (0..31)
	            str += date.getDate();
	            break;
	          case 'H': // Hour (00..23)
	            t = date.getHours();
	            if ( t < 10 ) str += '0';
	            str += String(t);
	            break;
	          case 'h': // Hour (01..12)
	          case 'I': // Hour (01..12)
	            t = date.getHours() % 12;
	            if ( t == 0 )
	              str += '12';
	            else
	            {
	              if ( t < 10 ) str += '0';
	              str += String(t);
	            }
	            break;
	          case 'i': // Minutes, numeric (00..59)
	            t = date.getMinutes();
	            if ( t < 10 ) str += '0';
	            str += String(t);
	            break;
	          case 'k': // Hour (0..23)
	            str += date.getHours();
	            break;
	          case 'l': // Hour (1..12)
	            t = date.getHours() % 12;
	            if ( t == 0 )
	              str += '12';
	            else
	              str += String(t);
	            break;
	          case 'M': // Month name (January..December)
	            str += this.mNames[ date.getMonth() ];
	            break;
	          case 'm': // Month, numeric (00..12)
	            t = date.getMonth() + 1;
	            if ( t < 10 ) str += '0';
	            str += String(t);
	            break;
	          case 'p': // AM or PM
	            str += ( ( date.getHours() < 12 ) ? 'AM' : 'PM' );
	            break;
	          case 'r': // Time, 12-hour (hh:mm:ss followed by AM or PM)
	            t = date.getHours() % 12;
	            if ( t == 0 )
	              str += '12:';
	            else
	            {
	              if ( t < 10 ) str += '0';
	              str += String(t) + ':';
	            }
	            t = date.getMinutes();
	            if ( t < 10 ) str += '0';
	            str += String(t) + ':';
	            t = date.getSeconds();
	            if ( t < 10 ) str += '0';
	            str += String(t);
	            str += ( ( date.getHours() < 12 ) ? 'AM' : 'PM' );
	            break;
	          case 'S': // Seconds (00..59)
	          case 's': // Seconds (00..59)
	            t = date.getSeconds();
	            if ( t < 10 ) str += '0';
	            str += String(t);
	            break;
	          case 'T': // Time, 24-hour (hh:mm:ss)
	            t = date.getHours();
	            if ( t < 10 ) str += '0';
	            str += String(t) + ':';
	            t = date.getMinutes();
	            if ( t < 10 ) str += '0';
	            str += String(t) + ':';
	            t = date.getSeconds();
	            if ( t < 10 ) str += '0';
	            str += String(t);
	            break;
	          case 'W': // Weekday name (Sunday..Saturday)
	            str += this.dNames[ date.getDay() ];
	            break;
	          case 'w': // Day of the week (0=Sunday..6=Saturday)
	            str += date.getDay();
	            break;
	          case 'Y': // Year, numeric, four digits (negative if before 0001)
	            str += AnyTime.pad(date.getFullYear(),4);
	            break;
	          case 'y': // Year, numeric (two digits, negative if before 0001)
	            t = date.getFullYear() % 100;
	            str += AnyTime.pad(t,2);
	            break;
	          case 'Z': // Year, numeric, four digits, unsigned (NON-MYSQL)
	            str += AnyTime.pad(Math.abs(date.getFullYear()),4);
	            break;
	          case 'z': // Year, numeric, variable length, unsigned (NON-MYSQL)
	            str += Math.abs(date.getFullYear());
	            break;
	          case '%': // A literal '%' character
	            str += '%';
	            break;
	          case 'f': // Microseconds (000000..999999)
	          case 'j': // Day of year (001..366)
	          case 'U': // Week (00..53), where Sunday is the first day of the week
	          case 'u': // Week (00..53), where Monday is the first day of the week
	          case 'V': // Week (01..53), where Sunday is the first day of the week; used with %X
	          case 'v': // Week (01..53), where Monday is the first day of the week; used with %x
	          case 'X': // Year for the week where Sunday is the first day of the week, numeric, four digits; used with %V
	          case 'x': // Year for the week, where Monday is the first day of the week, numeric, four digits; used with %v
	            throw '%'+this.fmt.charAt(f)+' not implemented by AnyTime.Converter';
	          default: // for any character not listed above
	            str += this.fmt.substr(f,2);
	        } // switch ( this.fmt.charAt(f+1) )
	        f++;
	      } // else
	    } // for ( var f = 0 ; f < _flen ; f++ )
	    return str;
	    
	}; // AnyTime.Converter.format()
	  
	//-------------------------------------------------------------------------
	//  AnyTime.Converter.parse() returns a Date initialized from a specified
	//  string, using the format passed to AnyTime.Converter().
	//
	//  Method parameter:
	//
	//    str - the String object to be converted
	//-------------------------------------------------------------------------
	
	this.parse = function( str )
	{
	    var era = 1;
	    var time = new Date();
	    var slen = str.length;
	    var s = 0;
	    var i, matched, sub, sublen, temp;
	    for ( var f = 0 ; f < _flen ; f++ )
	    {
	      if ( this.fmt.charAt(f) == '%' )
	      {
	        switch ( this.fmt.charAt(f+1) )
	        {
	          case 'a': // Abbreviated weekday name (Sun..Sat)
	            matched = false;
	            for ( sublen = 0 ; s + sublen < slen ; sublen++ )
	            {
	              sub = str.substr(s,sublen);
	              for ( i = 0 ; i < 12 ; i++ )
	                if ( this.dAbbr[i] == sub )
	                {
	                  matched = true;
	                  s += sublen;
	                  break;
	                }
	              if ( matched )
	                break;
	            } // for ( sublen ... )
	            if ( ! matched )
	              throw 'unknown weekday: '+str.substr(s);
	            break;
	          case 'B': // BCE string (eAbbr[0]), only if needed. (NON-MYSQL)
	            sublen = this.eAbbr[0].length;
	            if ( ( s + sublen <= slen ) && ( str.substr(s,sublen) == this.eAbbr[0] ) )
	            {
	              era = (-1);
	              s += sublen;
	            }
	            break;
	          case 'b': // Abbreviated month name (Jan..Dec)
	            matched = false;
	            for ( sublen = 0 ; s + sublen < slen ; sublen++ )
	            {
	              sub = str.substr(s,sublen);
	              for ( i = 0 ; i < 12 ; i++ )
	                if ( this.mAbbr[i] == sub )
	                {
	                  time.setMonth( i );
	                  matched = true;
	                  s += sublen;
	                  break;
	                }
	              if ( matched )
	                break;
	            } // for ( sublen ... )
	            if ( ! matched )
	              throw 'unknown month: '+str.substr(s);
	            break;
	          case 'C': // CE string (eAbbr[1]), only if needed. (NON-MYSQL)
	            sublen = this.eAbbr[1].length;
	            if ( ( s + sublen <= slen ) && ( str.substr(s,sublen) == this.eAbbr[1] ) )
	              s += sublen; // note: CE is the default era
	            break;
	          case 'c': // Month, numeric (0..12)
	            if ( ( s+1 < slen ) && this.dAt(str,s+1) )
	            {
	              time.setMonth( (Number(str.substr(s,2))-1)%12 );
	              s += 2;
	            }
	            else
	            {
	              time.setMonth( (Number(str.substr(s,1))-1)%12 );
	              s++;
	            }
	            break;
	          case 'D': // Day of the month with English suffix (0th,1st,...)
	            if ( ( s+1 < slen ) && this.dAt(str,s+1) )
	            {
	              time.setDate( Number(str.substr(s,2)) );
	              s += 4;
	            }
	            else
	            {
	              time.setDate( Number(str.substr(s,1)) );
	              s += 3;
	            }
	            break;
	          case 'd': // Day of the month, numeric (00..31)
	            time.setDate( Number(str.substr(s,2)) );
	            s += 2;
	            break;
	          case 'E': // era string (from eAbbr[]) (NON-MYSQL)
	            sublen = this.eAbbr[0].length;
	            if ( ( s + sublen <= slen ) && ( str.substr(s,sublen) == this.eAbbr[0] ) )
	            {
	              era = (-1);
	              s += sublen;
	            }
	            else if ( ( s + ( sublen = this.eAbbr[1].length ) <= slen ) && ( str.substr(s,sublen) == this.eAbbr[1] ) )
	              s += sublen; // note: CE is the default era
	            else
	              throw 'unknown era: '+str.substr(s);
	            break;
	          case 'e': // Day of the month, numeric (0..31)
	            if ( ( s+1 < slen ) && this.dAt(str,s+1) )
	            {
	              time.setDate( Number(str.substr(s,2)) );
	              s += 2;
	            }
	            else
	            {
	              time.setDate( Number(str.substr(s,1)) );
	              s++;
	            }
	            break;
	          case 'f': // Microseconds (000000..999999)
	            s += 6; // SKIPPED!
	            break;
	          case 'H': // Hour (00..23)
	            time.setHours( Number(str.substr(s,2)) );
	            s += 2;
	            break;
	          case 'h': // Hour (01..12)
	          case 'I': // Hour (01..12)
	            time.setHours( Number(str.substr(s,2)) );
	            s += 2;
	            break;
	          case 'i': // Minutes, numeric (00..59)
	            time.setMinutes( Number(str.substr(s,2)) );
	            s += 2;
	            break;
	          case 'k': // Hour (0..23)
	            if ( ( s+1 < slen ) && this.dAt(str,s+1) )
	            {
	              time.setHours( Number(str.substr(s,2)) );
	              s += 2;
	            }
	            else
	            {
	              time.setHours( Number(str.substr(s,1)) );
	              s++;
	            }
	            break;
	          case 'l': // Hour (1..12)
	            if ( ( s+1 < slen ) && this.dAt(str,s+1) )
	            {
	              time.setHours( Number(str.substr(s,2)) );
	              s += 2;
	            }
	            else
	            {
	              time.setHours( Number(str.substr(s,1)) );
	              s++;
	            }
	            break;
	          case 'M': // Month name (January..December)
	            matched = false;
	            for (sublen=_shortMon ; s + sublen <= slen ; sublen++ )
	            {
	              if ( sublen > _longMon )
	                break;
	              sub = str.substr(s,sublen);
	              for ( i = 0 ; i < 12 ; i++ )
	              {
	                if ( this.mNames[i] == sub )
	                {
	                  time.setMonth( i );
	                  matched = true;
	                  s += sublen;
	                  break;
	                }
	              }
	              if ( matched )
	                break;
	            }
	            break;
	          case 'm': // Month, numeric (00..12)
	            time.setMonth( (Number(str.substr(s,2))-1)%12 );
	            s += 2;
	            break;
	          case 'p': // AM or PM
	            if ( str.charAt(s) == 'P' )
	            {
	              if ( time.getHours() == 12 )
	                time.setHours(0);
	              else
	                time.setHours( time.getHours() + 12 );
	            }
	            s += 2;
	            break;
	          case 'r': // Time, 12-hour (hh:mm:ss followed by AM or PM)
	            time.setHours(Number(str.substr(s,2)));
	            time.setMinutes(Number(str.substr(s+3,2)));
	            time.setSeconds(Number(str.substr(s+6,2)));
	            if ( str.substr(s+8,1) == 'P' )
	            {
	              if ( time.getHours() == 12 )
	                time.setHours(0);
	              else
	                time.setHours( time.getHours() + 12 );
	            }
	            s += 10;
	            break;
	          case 'S': // Seconds (00..59)
	          case 's': // Seconds (00..59)
	            time.setSeconds(Number(str.substr(s,2)));
	            s += 2;
	            break;
	          case 'T': // Time, 24-hour (hh:mm:ss)
	            time.setHours(Number(str.substr(s,2)));
	            time.setMinutes(Number(str.substr(s+3,2)));
	            time.setSeconds(Number(str.substr(s+6,2)));
	            s += 8;
	            break;
	          case 'W': // Weekday name (Sunday..Saturday)
	            matched = false;
	            for (sublen=_shortDay ; s + sublen <= slen ; sublen++ )
	            {
	              if ( sublen > _longDay )
	                break;
	              sub = str.substr(s,sublen);
	              for ( i = 0 ; i < 7 ; i++ )
	              {
	                if ( this.dNames[i] == sub )
	                {
	                  matched = true;
	                  s += sublen;
	                  break;
	                }
	              }
	              if ( matched )
	                break;
	            }
	            break;
	          case 'Y': // Year, numeric, four digits, negative if before 0001
	            i = 4;
	            if ( str.substr(s,1) == '-' )
	              i++;
	            time.setFullYear(Number(str.substr(s,i)));
	            s += i;
	            break;
	          case 'y': // Year, numeric (two digits), negative before baseYear
	            i = 2;
	            if ( str.substr(s,1) == '-' )
	              i++;
	            temp = Number(str.substr(s,i));
	            if ( typeof(this.baseYear) == 'number' )
	            	temp += this.baseYear;
	            else if ( temp < 70 )
	            	temp += 2000;
	            else
	            	temp += 1900;
	            time.setFullYear(temp);
	            s += i;
	            break;
	          case 'Z': // Year, numeric, four digits, unsigned (NON-MYSQL)
	            time.setFullYear(Number(str.substr(s,4)));
	            s += 4;
	            break;
	          case 'z': // Year, numeric, variable length, unsigned (NON-MYSQL)
	            i = 0;
	            while ( ( s < slen ) && this.dAt(str,s) )
	              i = ( i * 10 ) + Number(str.charAt(s++));
	            time.setFullYear(i);
	            break;
	          case 'j': // Day of year (001..366)
	          case 'U': // Week (00..53), where Sunday is the first day of the week
	          case 'u': // Week (00..53), where Monday is the first day of the week
	          case 'V': // Week (01..53), where Sunday is the first day of the week; used with %X
	          case 'v': // Week (01..53), where Monday is the first day of the week; used with %x
	          case 'w': // Day of the week (0=Sunday..6=Saturday)
	          case 'X': // Year for the week where Sunday is the first day of the week, numeric, four digits; used with %V
	          case 'x': // Year for the week, where Monday is the first day of the week, numeric, four digits; used with %v
	            throw '%'+this.fmt.charAt(f+1)+' not implemented by AnyTime.Converter';
	          case '%': // A literal '%' character
	          default: // for any character not listed above
	            s++;
	            break;
	        }
	        f++;
	      } // if ( this.fmt.charAt(f) == '%' )
	      else if ( this.fmt.charAt(f) != str.charAt(s) )
	        throw str + ' is not in "' + this.fmt + '" format';
	      else
	        s++;
	    } // for ( var f ... )
	    if ( era < 0 )
	      time.setFullYear( 0 - time.getFullYear() );
	    return time;
	    
	}; // AnyTime.Converter.parse()
	
	//-------------------------------------------------------------------------
	//	AnyTime.Converter construction code:
	//-------------------------------------------------------------------------
	  
	(function(_this)
	{
	  	var i;
		
	  	if ( ! options )
	  		options = {};
		
	  	if ( options['baseYear'] )
			_this.baseYear = Number(options['baseYear']);
		
	  	if ( options['format'] )
			_this.fmt = options['format'];
		
	  	_flen = _this.fmt.length;
		
	  	if ( options['dayAbbreviations'] )
	  		_this.dAbbr = $.makeArray( options['dayAbbreviations'] );
		
	  	if ( options['dayNames'] )
	  	{
	  		_this.dNames = $.makeArray( options['dayNames'] );
	  		_longDay = 1;
	  		_shortDay = 1000;
	  		for ( i = 0 ; i < 7 ; i++ )
	  		{
				var len = _this.dNames[i].length;
				if ( len > _longDay )
					_longDay = len;
				if ( len < _shortDay )
					_shortDay = len;
	  		}
	  	}
		
	  	if ( options['eraAbbreviations'] )
	  		_this.eAbbr = $.makeArray(options['eraAbbreviations']);
		
	  	if ( options['monthAbbreviations'] )
	  		_this.mAbbr = $.makeArray(options['monthAbbreviations']);
		
	  	if ( options['monthNames'] )
	  	{
	  		_this.mNames = $.makeArray( options['monthNames'] );
	  		_longMon = 1;
	  		_shortMon = 1000;
	  		for ( i = 0 ; i < 12 ; i++ )
	  		{
	  			var len = _this.mNames[i].length;
	  			if ( len > _longMon )
					_longMon = len;
	  			if ( len < _shortMon )
	  				_shortMon = len;
	  		}
	  	}
		
	  	for ( i = 0 ; i < 12 ; i++ )
	  	{
	  		_this.mNums[ _this.mAbbr[i] ] = i;
	  		_this.mNums[ _this.mNames[i] ] = i;
	  	}
		
	  	for ( i = 0 ; i < 7 ; i++ )
	  	{
	  		_this.dNums[ _this.dAbbr[i] ] = i;
	  		_this.dNums[ _this.dNames[i] ] = i;
	  	}
		
	})(this); // AnyTime.Converter construction

}, // AnyTime.Converter()

//=============================================================================
//  AnyTime.pad() pads a value with a specified number of zeroes and returns
//  a string containing the padded value.
//=============================================================================

pad: function( val, len )
{
	var str = String(Math.abs(val));
	while ( str.length < len )
	str = '0'+str;
	if ( val < 0 )
	str = '-'+str;
	return str;
},

//=============================================================================
//  AnyTime.widget()
//
//  Creates a date/time entry widget attached to a specified text field.
//  Instead of entering a date and/or time into the text field, the user
//  selects legal combinations using the widget, and the field is auto-
//  matically populated.  The widget can be incorporated into the page
//	"inline", or used as a "popup" that appears when the text field is
//  clicked and disappears when the widget is dismissed. Ajax can be used
//  to send the selected value to a server to approve or veto it.
//
//  To create a widget, simply include the necessary files in an HTML page
//  and call the function for each date/time input field.  The following
//  example creates a popup widget for field "foo" using the default
//  format, and a second date-only (no time) inline (always-visible)
//  Ajax-enabled widget for field "bar":
//
//    <link rel="stylesheet" type="text/css" href="anytime.css" />
//    <script type="text/javascript" src="jquery.js"></script>
//    <script type="text/javascript" src="anytime.js"></script>
//    <input type="text" id="foo" tabindex="1" value="1967-07-30 23:45" />
//    <input type="text" id="bar" tabindex="2" value="01/06/90" />
//    <script type="text/javascript">
//      AnyTime.widget( "foo" );
//      AnyTime.widget( "bar", { placement:"inline", format: "%m/%d/%y",
//								ajaxOptions { url: "/some/server/page/" } } );
//    </script>
//
//  The appearance of the widget can be extensively modified using CSS styles.
//  A default appearance can be achieved by the "anytime.css" stylesheet that
//  accompanies this script.  The default style looks better in browsers other
//  than Internet Explorer (before IE8) because older versions of IE do not
//  properly implement the CSS box model standard; however, it is passable in
//  Internet Explorer as well.
//
//  Method parameters:
//
//  id - the "id" attribute of the textfield to associate with the
//    AnyTime.Widget object.  The AnyTime.Widget will attach itself
//    to the textfield and manage its value.
//
//  options - an object (associative array) of optional parameters that
//    override default behaviors.  The supported options are:
//
//    ajaxOptions - options passed to jQuery's $.ajax() method whenever
//      the user dismisses a popup widget or selects a value in an inline
//      widget.  The input's name (or ID) and value are passed to the
//      server (appended to ajaxOptions.data, if present), and the
//      "success" handler sets the input's value to the responseText.
//      Therefore, the text returned by the server must be valid for the
//      input'sdate/time format, and the server can approve or veto the
//      value chosen by the user. For more information, see:
//      http://docs.jquery.com/Ajax.
//      If ajaxOptions.success is specified, it is used instead of the
//      default "success" behavior.
//
//    askEra - if true, buttons to select the era are shown on the year
//        selector popup, even if format specifier does not include the
//        era.  If false, buttons to select the era are NOT shown, even
//        if the format specifier includes ther era.  Normally, era buttons
//        are only shown if the format string specifies the era.
//
//    askSecond - if false, buttons for number-of-seconds are not shown
//        even if the format includes seconds.  Normally, the buttons
//        are shown if the format string includes seconds.
//
//    earliest - String or Date object representing the earliest date/time
//        that a user can select.  For best results if the field is only
//        used to specify a date, be sure to set the time to 00:00:00.
//        If a String is used, it will be parsed according to the widget's
//        format (see AnyTime.Converter.format()).
//
//    firstDOW - a value from 0 (Sunday) to 6 (Saturday) stating which
//      day should appear at the beginning of the week.  The default is 0
//      (Sunday).  The most common substitution is 1 (Monday).  Note that
//      if custom arrays are specified for AnyTime.Converter's dayAbbreviations
//      and/or dayNames options, they should nonetheless begin with the
//      value for Sunday.
//
//    hideInput - if true, the <input> is "hidden" (the widget appears in 
//      its place). This actually sets the border, height, margin, padding
//      and width of the field as small as possivle, so it can still get focus.
//      If you try to hide the field using traditional techniques (such as
//      setting "display:none"), the widget will not behave correctly.
//
//    labelDayOfMonth - the label for the day-of-month "buttons".
//      Can be any HTML!  If not specified, "Day of Month" is assumed.
//
//    labelDismiss - the label for the dismiss "button" (if placement is
//      "popup"). Can be any HTML!  If not specified, "X" is assumed.
//
//    labelHour - the label for the hour "buttons".
//      Can be any HTML!  If not specified, "Hour" is assumed.
//
//    labelMinute - the label for the minute "buttons".
//      Can be any HTML!  If not specified, "Minute" is assumed.
//
//    labelMonth - the label for the month "buttons".
//      Can be any HTML!  If not specified, "Month" is assumed.
//
//    labelSecond - the label for the second "buttons".
//      Can be any HTML!  If not specified, "Second" is assumed.
//      This option is ignored if askSecond is false!
//
//    labelTitle - the label for the "title bar".  Can be any HTML!
//      If not specified, then whichever of the following is most
//      appropriate is used:  "Select a Date and Time", "Select a Date"
//      or "Select a Time", or no label if only one field is present.
//
//    labelYear - the label for the year "buttons".
//      Can be any HTML!  If not specified, "Year" is assumed.
//
//    latest - String or Date object representing the latest date/time
//        that a user can select.  For best results if the field is only
//        used to specify a date, be sure to set the time to 23:59:59.
//        If a String is used, it will be parsed according to the widget's
//        format (see AnyTime.Converter.format()).
//
//    placement - One of the following strings:
//
//      "popup" = the widget appears above its <input> when the input
//        receives focus, and disappears when it is dismissed.  This is
//        the default behavior.
//
//      "inline" = the widget is placed immediately after the <input>
//        and remains visible at all times.  When choosing this placement,
//        it is best to make the <input> invisible and use only the
//        widget to select dates.  The <input> value can still be used
//        during form submission as it will always reflect the current
//        widget state.
//
//        WARNING: when using "inline" and XHTML and including a day-of-
//        the-month format field, the input may only appear where a <table>
//        element is permitted (for example, NOT within a <p> element).
//        This is because the widget uses a <table> element to arrange
//        the day-of-the-month (calendar) buttons.  Failure to follow this
//        advice may result in an "unknown error" in Internet Explorer.
//
//    The following additional options may be specified; see documentation
//    for AnyTime.Converter (above) for information about these options:
//
//      baseYear
//      dayAbbreviations
//      dayNames
//      eraAbbreviations
//      format
//      monthAbbreviations
//      monthNames
//
//  Other behavior, such as how to format the values on the display
//  and which "buttons" to include, is inferred from the format string.
//=============================================================================

widget: function( id, options )
{
	//  Create a new private object instance to manage the widget,
	//  if one does not already exist.
	
    if ( __widgets[id] )
    	throw 'Cannot create another AnyTime widget for "'+id+'"';

	var _this = null;

	__widgets[id] =
	{
		//  private members
		
		twelveHr: false,
		ajaxOpts: null,		// options for AJAX requests
		denyTab: true,      // set to true to stop Opera from tabbing away
		askEra: false,		// prompt the user for the era in yDiv?
		cloak: null,		// cloak div
		conv: null,			// AnyTime.Converter
	  	bMinW: 0,			// min width of body div
	  	bMinH: 0,			// min height of body div
	  	dMinW: 0,    		// min width of date div
	  	dMinH: 0,			// min height of date div
		div: null,			// widget div
	  	dB: null,			// body div
	  	dD: null,			// date div
	  	dY: null,			// years div
	  	dMo: null,			// months div
	  	dDoM: null,			// date-of-month table
	  	hDoM: null,			// date-of-month heading
	  	hMo: null,			// month heading
	  	hTitle: null,		// title heading
	  	hY: null,			// year heading
	  	dT: null,			// time div
	  	dH: null,			// hours div
	  	dM: null,			// minutes div
	  	dS: null,			// seconds div
		earliest: null,		// earliest selectable date/time
		fBtn: null,			// button with current focus
		fDOW: 0,			// index to use as first day-of-week
		id: null,			// widget ID
		inp: null,			// input text field
		latest: null,		// latest selectable date/time
		lastAjax: null,		// last value submitted using AJAX
		lostFocus: false,	// when focus is lost, must redraw
		lX: 'X',			// label for dismiss button
		lY: 'X',			// label for year
		pop: true,			// widget is a popup?
		time: null,			// current date/time
	  	tMinW: 0,			// min width of time div
	  	tMinH: 0,			// min height of time div
		url: null,			// URL to submit value using AJAX
	  	wMinW: 0,			// min width of widget
	  	wMinH: 0,			// min height of widget
		yAhead: null,		// years-ahead button
		y0XXX: null,		// millenium-digit-zero button (for focus)
		yCur: null,			// current-year button
		yDiv: null,			// year selector popup
		yLab: null,			// year label
		yNext: null,		// next-year button
		yPast: null,		// years-past button
		yPrior: null,		// prior-year button

		//---------------------------------------------------------------------
		//  .initialize() initializes the widget instance.
		//---------------------------------------------------------------------

		initialize: function( id )
		{
			_this = this;

			this.id = 'Atw_'+id;

		  	if ( ! options )
		  		options = {};

		  	this.conv = new AnyTime.Converter(options);

		  	if ( options.placement )
		  	{
		  		if ( options.placement == 'inline' )
		  			this.pop = false;
		  		else if ( options.placement != 'popup' )
		  			throw 'unknown placement: ' + options.placement;
		  	}

		  	if ( options.ajaxOptions )
		  	{
		  		this.ajaxOpts = jQuery.extend( {}, options.ajaxOptions );
		        if ( ! this.ajaxOpts.success )
		        	this.ajaxOpts.success = function(data,status) { _this.inp[0].value = data; };
		  	}
		    
		  	if ( options.earliest )
		  	{
		  		if ( typeof options.earliest.getTime == 'function' )
		  			this.earliest = options.earliest.getTime();
		  		else
		  			this.earliest = this.conv.parse( options.earliest.toString() );
		  	}

		  	if ( options.firstDOW )
		  	{
		  		if ( ( options.firstDOW < 0 ) || ( options.firstDOW > 6 ) )
		  			throw new Exception('illegal firstDOW: ' + options.firstDOW); 
		  		this.fDOW = options.firstDOW;
		  	}

		  	if ( options.latest )
		  	{
		  		if ( typeof options.latest.getTime == 'function' )
		  			this.latest = options.latest.getTime();
		  		else
		  			this.latest = this.conv.parse( options.earliest.toString() );
		  	}

		  	this.lX = options.labelDismiss || 'X';
		  	this.lY = options.labelYear || 'Year';

		  	//  Infer what we can about what to display from the format.

		  	var i;
		  	var t;
		  	var lab;
		  	var shownFields = 0;
		  	var format = this.conv.fmt;

		  	if ( typeof options.askEra != 'undefined' )
		  		this.askEra = options.askEra;
		  	else
		  		this.askEra = (format.indexOf('%B')>=0) || (format.indexOf('%C')>=0) || (format.indexOf('%E')>=0);
		  	var askYear = (format.indexOf('%Y')>=0) || (format.indexOf('%y')>=0) || (format.indexOf('%Z')>=0) || (format.indexOf('%z')>=0);
		  	var askMonth = (format.indexOf('%b')>=0) || (format.indexOf('%c')>=0) || (format.indexOf('%M')>=0) || (format.indexOf('%m')>=0);
		  	var askDoM = (format.indexOf('%D')>=0) || (format.indexOf('%d')>=0) || (format.indexOf('%e')>=0);
		  	var askDate = askYear || askMonth || askDoM;
		  	this.twelveHr = (format.indexOf('%h')>=0) || (format.indexOf('%I')>=0) || (format.indexOf('%l')>=0) || (format.indexOf('%r')>=0);
		  	var askHour = this.twelveHr || (format.indexOf('%H')>=0) || (format.indexOf('%k')>=0) || (format.indexOf('%T')>=0);
		  	var askMinute = (format.indexOf('%i')>=0) || (format.indexOf('%r')>=0) || (format.indexOf('%T')>=0);
		  	var askSec = ( (format.indexOf('%r')>=0) || (format.indexOf('%S')>=0) || (format.indexOf('%s')>=0) || (format.indexOf('%T')>=0) );
		  	if ( askSec && ( typeof options.askSecond != 'undefined' ) )
		  		askSec = options.askSecond;
		  	var askTime = askHour || askMinute || askSec;


		  	//  Create the widget HTML and add it to the page.
		  	//  Popup widgets will be moved to the end of the body
		  	//  once the entire page has loaded.

		  	this.inp = $('#'+id);
		  	this.div = $( '<div class="AtwWindow AtwWidget ui-widget ui-widget-content ui-corner-all" style="width:0;height:0" id="' + this.id + '"/>' );
		    this.inp.after(this.div);
		  	this.wMinW = this.div.outerWidth(!$.browser.safari);
		  	this.wMinH = this.div.AtwHeight(true);
		  	this.hTitle = $( '<h5 class="AtwTitle ui-widget-header ui-corner-top"/>' ); 
		  	this.div.append( this.hTitle );
		  	this.dB = $( '<div class="AtwBody" style="width:0;height:0"/>' );
		  	this.div.append( this.dB );
		  	this.bMinW = this.dB.outerWidth(true);
		  	this.bMinH = this.dB.AtwHeight(true);

		  	if ( options.hideInput )
		        this.inp.css({border:0,height:'1px',margin:0,padding:0,width:'1px'});
		  	
		  	//  Add dismiss box to title (if popup)

		  	var t = null;
		  	var xDiv = null;
		  	if ( this.pop )
		  	{
		  		xDiv = $( '<div class="AtwDismissBtn ui-state-default">'+this.lX+'</div>' );
		  		this.hTitle.append( xDiv );
		  		xDiv.click(function(e){_this.dismiss(e);});
		  	}

		  	//  date (calendar) portion

		  	var lab = '';
		  	
		  	if ( askDate )
		  	{
			  this.dD = $( '<div class="AtwDate" style="width:0;height:0"/>' );
			  this.dB.append( this.dD );
		  	  this.dMinW = this.dD.outerWidth(true);
		  	  this.dMinH = this.dD.AtwHeight(true);

		      if ( askYear )
		      {
		    	  this.yLab = $('<h6 class="AtwLbl AtwLblYr">' + this.lY + '</h6>');
		    	  this.dD.append( this.yLab );

		          this.dY = $( '<ul class="AtwYrs ui-helper-reset" />' );
		          this.dD.append( this.dY );

		          this.yPast = this.btn(this.dY,'&lt;',this.newYear,['AtwYrsPast'],'- '+this.lY);
		          this.yPrior = this.btn(this.dY,'1',this.newYear,['AtwYrPrior'],'-1 '+this.lY);
		          this.yCur = this.btn(this.dY,'2',this.newYear,['AtwYrCurrent'],this.lY);
		          this.yCur.removeClass('ui-state-default');
		          this.yCur.addClass('AtwCurrentBtn ui-state-default ui-state-highlight');

		          this.yNext = this.btn(this.dY,'3',this.newYear,['AtwYrNext'],'+1 '+this.lY);
		          this.yAhead = this.btn(this.dY,'&gt;',this.newYear,['AtwYrsAhead'],'+ '+this.lY);
		          
		          shownFields++;

		      } // if ( askYear )

		      if ( askMonth )
		      {
		    	  lab = options.labelMonth || 'Month';
		    	  this.hMo = $( '<h6 class="AtwLbl AtwLblMonth">' + lab + '</h6>' );
		    	  this.dD.append( this.hMo );
		    	  this.dMo = $('<ul class="AtwMons" />');
		    	  this.dD.append(this.dMo);
		    	  for ( i = 0 ; i < 12 ; i++ )
		    		  this.btn( this.dMo, this.conv.mAbbr[i], 
			    			function( event )
			    			{
			    				var elem = $(event.target);
			    				var mo = this.conv.mNums[elem.text()];
			    				var t = new Date(this.time.getTime());
			    				if ( t.getDate() > __daysIn[mo] )
			    					t.setDate(__daysIn[mo])
			    				t.setMonth(mo);
			    				if ( this.set(t) )
			    					this.upd(elem);
			    			},
			    			['AtwMon','AtwMon'+String(i+1)], lab+' '+this.conv.mNames[i] );
		    	  shownFields++;
		      }

		      if ( askDoM )
		      {
		    	lab = options.labelDayOfMonth || 'Day of Month';
		        this.hDoM = $('<h6 class="AtwLbl AtwLblDoM">' + lab + '</h6>' );
		      	this.dD.append( this.hDoM );
		        this.dDoM =  $( '<table border="0" cellpadding="0" cellspacing="0" class="AtwDoMTable"/>' );
		        this.dD.append( this.dDoM );
		        t = $( '<thead class="AtwDoMHead" role="columnheader"/>' );
		        this.dDoM.append(t);
		        var tr = $( '<tr class="AtwDoW"/>' );
		        t.append(tr);
		        for ( i = 0 ; i < 7 ; i++ )
		          tr.append( '<th class="AtwDoW AtwDoW'+String(i+1)+'">'+this.conv.dAbbr[(this.fDOW+i)%7]+'</th>' );

		        var tbody = $( '<tbody class="AtwDoMBody" />' );
		        this.dDoM.append(tbody);
		        for ( var r = 0 ; r < 6 ; r++ )
		        {
		          tr = $( '<tr class="AtwWk AtwWk'+String(r+1)+'"/>' );
		          tbody.append(tr);
		          for ( i = 0 ; i < 7 ; i++ )
		        	  this.btn( tr, 'x',
		        		function( event )
		        		{
		        			var elem = $(event.target);
		        			var dom = Number(elem.html());
		        			if ( dom )
		        			{
		        				var t = new Date(this.time.getTime());
		        				t.setDate(dom);
		        				if ( this.set(t) )
		        					this.upd( elem );
		        			}
		        		},
		        		['AtwDoM'], lab );
		        }
		        shownFields++;

		      } // if ( askDoM )

		    } // if ( askDate )

		    //  time portion

		    if ( askTime )
		    {
		      this.dT = $('<div class="AtwTime" style="width:0;height:0" />');
		      this.dB.append(this.dT);
		  	  this.tMinW = this.dT.outerWidth(true);
		  	  this.tMinH = this.dT.AtwHeight(true);

		      if ( askHour )
		      {
		        this.dH = $('<div class="AtwHrs"/>');
		        this.dT.append(this.dH);

		        lab = options.labelHour || 'Hour';
		        this.dH.append( $('<h6 class="AtwLbl AtwLblHr">'+lab+'</h6>') );
		        var amDiv = $('<ul class="AtwHrsAm"/>');
		        this.dH.append( amDiv );
		        var pmDiv = $('<ul class="AtwHrsPm"/>');
		        this.dH.append( pmDiv );

		        for ( i = 0 ; i < 12 ; i++ )
		        {
		          if ( this.twelveHr )
		          {
		            if ( i == 0 )
		              t = '12am';
		            else
		              t = String(i)+'am';
		          }
		          else
		            t = AnyTime.pad(i,2);

		          this.btn( amDiv, t, this.newHour,['AtwHr','AtwHr'+String(i)],lab+' '+t);

		          if ( this.twelveHr )
		          {
		            if ( i == 0 )
		              t = '12pm';
		            else
		              t = String(i)+'pm';
		          }
		          else
		            t = i+12;

		          this.btn( pmDiv, t, this.newHour,['AtwHr','AtwHr'+String(i+12)],lab+' '+t);
		        }

				shownFields++;
				
		      } // if ( askHour )

		      if ( askMinute )
		      {
		        this.dM = $('<div class="AtwMins"/>');
		        this.dT.append(this.dM);

		        lab = options.labelMinute || 'Minute';
		        this.dM.append( $('<h6 class="AtwLbl AtwLblMin">'+lab+'</h6>') );
		        var tensDiv = $('<ul class="AtwMinsTens"/>');
		        this.dM.append(tensDiv);

		        for ( i = 0 ; i < 6 ; i++ )
		          this.btn( tensDiv, i, 
		        		  function( event )
		        		  {
		        		      var elem = $(event.target);
		        		      var t = new Date(this.time.getTime());
		        		      t.setMinutes( (Number(elem.text())*10) + (this.time.getMinutes()%10) );
		        		      if ( this.set(t) )
		        		        this.upd(elem);
		        		  },
		        		  ['AtwMinTen','AtwMin'+i+'0'], lab+' '+i+'0' );

		        var onesDiv = $('<ul class="AtwMinsOnes"/>');
		        this.dM.append(onesDiv);
		        for ( i = 0 ; i < 10 ; i++ )
		          this.btn( onesDiv, i, 
		    		  function( event )
		    		  {
		    		      var elem = $(event.target);
		    		      var t = new Date(this.time.getTime());
		    		      t.setMinutes( (Math.floor(this.time.getMinutes()/10)*10)+Number(elem.text()) );
		    		      if ( this.set(t) )
		    		        this.upd(elem);  
		    		  },
		    		  ['AtwMinOne','AtwMin'+i], lab+' '+i );

				shownFields++;

		      } // if ( askMinute )

		      if ( askSec )
		      {
		        this.dS = $('<div class="AtwSecs"/>');
		        this.dT.append(this.dS);
		        lab = options.labelSecond || 'Second';
		        this.dS.append( $('<h6 class="AtwLbl AtwLblSec">'+lab+'</h6>') );
		        var tensDiv = $('<ul class="AtwSecsTens"/>');
		        this.dS.append(tensDiv);

		        for ( i = 0 ; i < 6 ; i++ )
		          this.btn( tensDiv, i,
		    		  function( event )
		    		  {
		    		      var elem = $(event.target);
		    		      var t = new Date(this.time.getTime());
		    		      t.setSeconds( (Number(elem.text())*10) + (this.time.getSeconds()%10) );
		    		      if ( this.set(t) )
		    		        this.upd(elem);
		    		  },
		    		  ['AtwSecTen','AtwSec'+i+'0'], lab+' '+i+'0' );

		        var onesDiv = $('<ul class="AtwSecsOnes"/>');
		        this.dS.append(onesDiv);
		        for ( i = 0 ; i < 10 ; i++ )
		          this.btn( onesDiv, i,
		    		  function( event )
		    		  {
		    		      var elem = $(event.target);
		    		      var t = new Date(this.time.getTime());
		    		      t.setSeconds( (Math.floor(this.time.getSeconds()/10)*10) + Number(elem.text()) );
		    		      if ( this.set(t) )
		    		        this.upd(elem);
		    		  },
		    		  ['AtwSecOne','AtwSec'+i], lab+' '+i );

				shownFields++;

		      } // if ( askSec )

		    } // if ( askTime )

		    //  Set the title.  If a title option has been specified, use it.
		    //  Otherwise, determine a worthy title based on which (and how many)
		    //  format fields have been specified.

		    if ( options.labelTitle )
		      this.hTitle.append( options.labelTitle );
		    else if ( shownFields > 1 )
		      this.hTitle.append( 'Select a '+(askDate?(askTime?'Date and Time':'Date'):'Time') );
		    else
		      this.hTitle.append( '&nbsp;' );


		    //  Initialize the widget's date/time value.

		    try
		    {
		      this.time = this.conv.parse(this.inp.get(0).value);
		    }
		    catch ( e )
		    {
		      this.time = new Date();
		    }
		    this.lastAjax = this.time;


		    //  If this is a popup widget, hide it until needed.

		    if ( this.pop )
		    {
		      this.div.hide();
		      if ( __iframe )
		        __iframe.hide();
		      this.div.css('position','absolute');
		    }
			
		    //  Setup event listeners for the input and resize listeners for
		    //  the widget.  Add the widget to the instances list (which is used
		    //  to hide widgets if the user clicks off of them).

		    this.inp.keydown(
		    	function(e)
		    	{
		    		_this.key(e);
		    	} );
		    
		    this.inp.keypress(
			    	function(e)
			    	{
			    		if ( $.browser.opera && _this.denyTab )
			    			e.preventDefault();
			    	} );
			    
		    this.inp.focus(
		    	function(e)
		    	{
		    		if ( _this.lostFocus )
		    			_this.showCal(e);
		    		_this.lostFocus = false;
		    	} );
		    
		    this.inp.blur(
		    	function(e)
		    	{
		    		_this.inpBlur(e);
		    	} );
		    
		    this.inp.click(
		    	function(e)
		    	{
		    		if ( _this.lostFocus )
		    			_this.showCal(e);
		    		_this.lostFocus = false;
		    	} );
		    
		    this.div.click( 
				function(e)
				{
					_this.lostFocus = false;
					_this.inp.focus();
				} );
		    
		    $(window).resize( 
		    	function(e)
		    	{
		    		_this.pos(e);
		    	} );
		    
		    if ( __initialized )
		    	this.onReady();

		}, // initialize()


		//---------------------------------------------------------------------
		//  .ajax() notifies the server of a value change using Ajax.
		//---------------------------------------------------------------------

		ajax: function()
		{
		    if ( this.ajaxOpts && ( this.time.getTime() != this.lastAjax.getTime() ) )
		    {
		      try
		      {
		    	var opts = jQuery.extend( {}, this.ajaxOpts );
		        if ( typeof opts.data == 'object' )
		        	opts.data[this.inp[0].name||this.inp[0].id] = this.inp[0].value;
		        else
		        {
		        	var opt = (this.inp[0].name||this.inp[0].id) + '=' + encodeURI(this.inp[0].value);
		        	if ( opts.data )
		        		opts.data += '&' + opt;
		        	else
		        		opts.data = opt;
		        }
		        $.ajax( opts );
		        this.lastAjax = this.time;
		      }
		      catch( e )
		      {
		      }
		    }
		    return;
		
		}, // .ajax()

		//---------------------------------------------------------------------
		//  .askYear() is called by this.newYear() when the yPast or yAhead
		//  button is clicked.
		//---------------------------------------------------------------------

		askYear: function( event )
		{
		    if ( ! this.yDiv )
		    {
		      this.cloak = $('<div class="AtwCloak" style="position:absolute" />');
		      this.div.append( this.cloak );
		
		      this.yDiv = $('<div class="AtwWindow AtwYrSelector ui-widget ui-widget-content ui-corner-all" style="position:absolute" />');
		      this.div.append(this.yDiv);
		
		      var title = $('<h5 class="AtwTitle AtwTitleYrSelector ui-widget-header ui-corner-top" />');
		      this.yDiv.append( title );
		
		      var xDiv = $('<div class="AtwDismissBtn ui-state-default">'+this.lX+'</div>');
		      title.append(xDiv);
		      xDiv.click(function(e){_this.dismissYDiv(e);});
		      this.cloak.click(function(e){_this.dismissYDiv(e);});
		
		      title.append( this.lY );
		
		      var yBody = $('<div class="AtwBody AtwBodyYrSelector" />');
		      var yW = yBody.AtwWidth(true);
		      var yH = 0;
		      this.yDiv.append( yBody );
		      
		      cont = $('<ul class="AtwYrMil" />' );
		      yBody.append(cont);
		      this.y0XXX = this.btn( cont, 0, this.newYPos,['AtwMil','AtwMil0'],this.lY+' '+0+'000');
		      for ( i = 1; i < 10 ; i++ )
		        this.btn( cont, i, this.newYPos,['AtwMil','AtwMil'+i],this.lY+' '+i+'000');
		      yW += cont.AtwWidth(true);
		      if ( yH < cont.AtwHeight(true) )
		    	  yH = cont.AtwHeight(true);
		
			  cont = $('<ul class="AtwYrCent" />' );
		      yBody.append(cont);
		      for ( i = 0 ; i < 10 ; i++ )
		        this.btn( cont, i, this.newYPos,['AtwCent','AtwCent'+i],this.lY+' '+i+'00');
		      yW += cont.AtwWidth(true);
		      if ( yH < cont.AtwHeight(true) )
		    	  yH = cont.AtwHeight(true);

		      cont = $('<ul class="AtwYrDec" />');
		      yBody.append(cont);
		      for ( i = 0 ; i < 10 ; i++ )
		        this.btn( cont, i, this.newYPos,['AtwDec','AtwDec'+i],this.lY+' '+i+'0');
		      yW += cont.AtwWidth(true);
		      if ( yH < cont.AtwHeight(true) )
		    	  yH = cont.AtwHeight(true);
		
		      cont = $('<ul class="AtwYrYr" />');
		      yBody.append(cont);
		      for ( i = 0 ; i < 10 ; i++ )
		        this.btn( cont, i, this.newYPos,['AtwYr','AtwYr'+i],this.lY+' '+i );
		      yW += cont.AtwWidth(true);
		      if ( yH < cont.AtwHeight(true) )
		    	  yH = cont.AtwHeight(true);
		
		      if ( this.askEra )
		      {
		        cont = $('<ul class="AtwYrEra" />' );
		        yBody.append(cont);
		
		        this.btn( cont, this.conv.eAbbr[0],
		        		function( event )
		        		{
	  		      			var t = new Date(this.time.getTime());
		        			var year = t.getFullYear();
		        		    if ( year > 0 )
								t.setFullYear(0-year);
							if ( this.set(t) )
							    this.updYDiv($(event.target));
		        		},
		        		['AtwEra','AtwBCE'], this.conv.eAbbr[0] );
		
		        this.btn( cont, this.conv.eAbbr[1],
		        		function( event )
		        		{
			      			var t = new Date(this.time.getTime());
		        			var year = t.getFullYear();
		        		    if ( year < 0 )
								t.setFullYear(0-year);
							if ( this.set(t) )
							    this.updYDiv($(event.target));
		        		},
		        		['AtwEra','AtwCE'], this.conv.eAbbr[1] );
		        
		        yW += cont.AtwWidth(true);
		        if ( yH < cont.AtwHeight(true) )
		        	yH = cont.AtwHeight(true);

		      } // if ( this.askEra )

		      if ( $.browser.msie ) // IE8+ThemeUI bug!
		    	  yW += 1;
			  yH += yBody.AtwHeight(true);
			  yBody.css('width',String(yW)+'px');
			  if ( ! __msie7 ) // IE7 bug!
				  yBody.css('height',String(yH)+'px');
		      if ( __msie6 || __msie7 ) // IE bugs!
		    	  title.width(String(yBody.outerWidth(true)+'px'));
		      yH += title.AtwHeight(true);
		      if ( title.AtwWidth(true) > yW )
		          yW = title.AtwWidth(true);
		      this.yDiv.css('width',String(yW)+'px');
			  if ( ! __msie7 ) // IE7 bug!
				  this.yDiv.css('height',String(yH)+'px');
		
		    } // if ( ! this.yDiv )
		    else
		    {
		      this.cloak.show();
		      this.yDiv.show();
		    }
		    this.pos(event);
		    this.updYDiv(null);
		    this.setFocus( this.yDiv.find('.AtwYrBtn.AtwCurrentBtn:first') );
		
		}, // .askYear()

		//---------------------------------------------------------------------
		//  .inpBlur() is called when a widget's input loses focus to dismiss
		//  the popup.  A 1/3 second delay is necessary to restore focus if
		//	the div is clicked (shorter delays don't always work!)
		//---------------------------------------------------------------------
  
		inpBlur: function(event)
		{
			this.lostFocus = true;
		    setTimeout(
		    	function()
		    	{ 
		    		if ( _this.lostFocus )
		    		{
		    			_this.div.find('.AtwFocusBtn').removeClass('AtwFocusBtn ui-state-focus'); 
		    			if ( _this.pop )
		    				_this.dismiss(event);
		    			else
		    				_this.ajax();
		    		}
		    	}, 334 );
		},
		
		//---------------------------------------------------------------------
		//  .btn() is called by AnyTime.Widget() to create a <div> element
		//  containing an <a> element.  The elements are given appropriate
		//  classes based on the specified "classes" (an array of strings).
		//	The specified "text" and "title" are used for the <a> element.
		//	The "handler" is bound to click events for the <div>, which will
		//	catch bubbling clicks from the <a> as well.  The button is
		//	appended to the specified parent (jQuery), and the <div> jQuery
		//	is returned.
		//---------------------------------------------------------------------
		
		btn: function( parent, text, handler, classes, title )
		{
			var tagName = ( (parent[0].nodeName.toLowerCase()=='ul')?'li':'td'); 
			var div$ = '<' + tagName +
			  				' class="AtwBtn';
			for ( var i = 0 ; i < classes.length ; i++ )
				div$ += ' ' + classes[i] + 'Btn';
			var div = $( div$ + ' ui-state-default">' + text + '</' + tagName + '>' );
			parent.append(div);
			  
			div.click(
			    function(e)
			  	{
			      // bind the handler to the widget so "this" is correct
				  _this.tempFunc = handler;
				  _this.tempFunc(e);
			  	});
		    return div;
		
		}, // .btn()
	
		//---------------------------------------------------------------------
		//  .dismiss() dismisses a popup widget.
		//---------------------------------------------------------------------
	
		dismiss: function(event)
		{
			this.ajax();
			this.div.hide();
			if ( __iframe )
				__iframe.hide();
			if ( this.yDiv )
				this.dismissYDiv();
			this.lostFocus = true;
		},
	
		//---------------------------------------------------------------------
		//  .dismissYDiv() dismisses the date selector popover.
		//---------------------------------------------------------------------
	
		dismissYDiv: function(event)
		{
		    this.yDiv.hide();
		    this.cloak.hide();
			this.setFocus(this.yCur);
		},
	
		//---------------------------------------------------------------------
		//  .setFocus() makes a specified psuedo-button appear to get focus.
		//---------------------------------------------------------------------
		
		setFocus: function(btn)
		{
			if ( ! btn.hasClass('AtwFocusBtn') )
			{
				this.div.find('.AtwFocusBtn').removeClass('AtwFocusBtn ui-state-focus');
				this.fBtn = btn;
				btn.removeClass('ui-state-default ui-state-highlight');
				btn.addClass('AtwFocusBtn ui-state-default ui-state-highlight ui-state-focus');
			}
		},
		  
		//---------------------------------------------------------------------
		//  .key() is invoked when a user presses a key while the widget's
		//	input has focus.  A psuedo-button is considered "in focus" and an
		//	appropriate action is performed according to the WAI-ARIA Authoring
		//	Practices 1.0 for datepicker from
		//  www.w3.org/TR/2009/WD-wai-aria-practices-20091215/#datepicker:
		//
		//  * LeftArrow moves focus left, continued to previous week.
		//  * RightArrow moves focus right, continued to next week.
		//  * UpArrow moves focus to the same weekday in the previous week.
		//  * DownArrow moves focus to same weekday in the next week.
		//  * PageUp moves focus to same day in the previous month.
		//  * PageDown moves focus to same day in the next month.
		//  * Shift+Page Up moves focus to same day in the previous year.
		//  * Shift+Page Down moves focus to same day in the next year.
		//  * Home moves focus to the first day of the month.
		//  * End moves focus to the last day of the month.
		//  * Ctrl+Home moves focus to the first day of the year.
		//  * Ctrl+End moves focus to the last day of the year.
		//  * Esc closes a DatePicker that is opened as a Popup.  
		//
		//  The following actions (for multiple-date selection) are NOT
		//	supported:
		//  * Shift+Arrow performs continous selection.  
		//  * Ctrl+Space multiple selection of certain days.
		//
		//  The authoring practices do not specify behavior for a time picker,
		//  or for month-and-year pickers that do not have a day-of-the-month,
		//  but AnyTime.Widget uses the following behavior to be as consistent
		//  as possible with the defined datepicker functionality:
		//  * LeftArrow moves focus left or up to previous value or field.
		//  * RightArrow moves focus right or down to next value or field.
		//  * UpArrow moves focus up or left to previous value or field.
		//  * DownArrow moves focus down or right to next value or field 
		//  * PageUp moves focus to the current value in the previous units
		//    (for example, from ten-minutes to hours or one-minutes to
		//	  ten-minutes or months to years).
		//  * PageDown moves focus to the current value in the next units
		//    (for example, from hours to ten-minutes or ten-minutes to 
		//    one-minutes or years to months).
		//  * Home moves the focus to the first unit button.
		//  * End moves the focus to the last unit button.
		//
		//  In addition, Tab and Shift+Tab move between units (including to/
		//	from the Day-of-Month table) and also in/out of the widget.
		//
		//  Because AnyTime.Widget sets a value as soon as the button receives
		//  focus, SPACE and ENTER are not needed (the WAI-ARIA guidelines use
		//  them to select a value.
		//---------------------------------------------------------------------
		
		key: function(event)
		{
			var t = null;
			var elem = this.div.find('.AtwFocusBtn');
		    var key = event.keyCode || event.which;
		    this.denyTab = true;

		    if ( key == 27 ) // ESC
		    {
		      if ( this.yDiv && this.yDiv.is(':visible') )
		        this.dismissYDiv(event);
		      else if ( this.pop )
			        this.dismiss(event);
		    }
		    else if ( ( key == 33 ) || ( ( key == 9 ) && event.shiftKey ) ) // PageUp & Shift+Tab
		    {
		    	if ( this.fBtn.hasClass('AtwMilBtn') )
				{
		    		if ( key == 9 )
				        this.dismissYDiv(event);
				}
		    	else if ( this.fBtn.hasClass('AtwCentBtn') )
					this.yDiv.find('.AtwMilBtn.AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwDecBtn') )
					this.yDiv.find('.AtwCentBtn.AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwYrBtn') )
					this.yDiv.find('.AtwDecBtn.AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwEraBtn') )
					this.yDiv.find('.AtwYrBtn.AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.parents('.AtwYrs').length )
				{
					if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwMonBtn') )
				{
					if ( this.dY )
						this.yCur.triggerHandler('click');
					else if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwDoMBtn') )
		    	{
		    		if ( ( key == 9 ) && event.shiftKey ) // Shift+Tab
					{
						this.denyTab = false;
						return;
					}
		    		else // PageUp
		    		{
			    		t = new Date(this.time.getTime());
				    	if ( event.shiftKey )
				    		t.setFullYear(t.getFullYear()-1);
				    	else
				    	{
				    		var mo = t.getMonth()-1;
		    				if ( t.getDate() > __daysIn[mo] )
		    					t.setDate(__daysIn[mo])
			    			t.setMonth(mo);
				    	}
			    		this.keyDateChange(t);
		    		}
		    	}
		    	else if ( this.fBtn.hasClass('AtwHrBtn') )
				{
		    		t = this.dDoM || this.dMo;
					if ( t )
						t.find(__acb).triggerHandler('click');
					else if ( this.dY )
						this.yCur.triggerHandler('click');
					else if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwMinTenBtn') )
				{
		    		t = this.dH || this.dDoM || this.dMo;
					if ( t )
						t.find(__acb).triggerHandler('click');
					else if ( this.dY )
						this.yCur.triggerHandler('click');
					else if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwMinOneBtn') )
					this.dM.find(__acb).triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwSecTenBtn') )
				{
		    		if ( this.dM )
		    			t = this.dM.find('.AtwMinsOnes');
		    		else
		    			t = this.dH || this.dDoM || this.dMo;
					if ( t )
						t.find(__acb).triggerHandler('click');
					else if ( this.dY )
						this.yCur.triggerHandler('click');
					else if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwSecOneBtn') )
					this.dS.find(__acb).triggerHandler('click');
			}
		    else if ( ( key == 34 ) || ( key == 9 ) ) // PageDown or Tab
		    {
		    	if ( this.fBtn.hasClass('AtwMilBtn') )
					this.yDiv.find('.AtwCentBtn.AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwCentBtn') )
					this.yDiv.find('.AtwDecBtn.AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwDecBtn') )
					this.yDiv.find('.AtwYrBtn.AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwYrBtn') )
		    	{
		    		t = this.yDiv.find('.AtwEraBtn.AtwCurrentBtn');
					if ( t.length )
						t.triggerHandler('click');
					else if ( key == 9 )
						this.dismissYDiv(event);
		    	}
		    	else if ( this.fBtn.hasClass('AtwEraBtn') )
		    	{
		    		if ( key == 9 )
		    			this.dismissYDiv(event);
		    	}
		    	else if ( this.fBtn.parents('.AtwYrs').length )
				{
		    		t = this.dDoM || this.dMo || this.dH || this.dM || this.dS; 
					if ( t )
						t.find(__acb).triggerHandler('click');
					else if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwMonBtn') )
				{
		    		t = this.dDoM || this.dH || this.dM || this.dS; 
					if ( t )
						t.find(__acb).triggerHandler('click');
					else if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwDoMBtn') )
		    	{
		    		if ( key == 9 ) // Tab
		    		{
		        		t = this.dH || this.dM || this.dS; 
		    			if ( t )
		    				t.find(__acb).triggerHandler('click');
		    			else
						{
							this.denyTab = false;
							return;
						}
		    		}
		    		else // PageDown
		    		{
			    		t = new Date(this.time.getTime());
				    	if ( event.shiftKey )
				    		t.setFullYear(t.getFullYear()+1);
				    	else
				    	{
				    		var mo = t.getMonth()+1;
		    				if ( t.getDate() > __daysIn[mo] )
		    					t.setDate(__daysIn[mo])
			    			t.setMonth(mo);
				    	}
			    		this.keyDateChange(t);
		    		}
		    	}
		    	else if ( this.fBtn.hasClass('AtwHrBtn') )
				{
		    		t = this.dM || this.dS; 
					if ( t )
						t.find(__acb).triggerHandler('click');
					else if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwMinTenBtn') )
		    		this.dM.find('.AtwMinsOnes .AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwMinOneBtn') )
				{
					if ( this.dS )
						this.dS.find(__acb).triggerHandler('click');
					else if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
		    	else if ( this.fBtn.hasClass('AtwSecTenBtn') )
		    		this.dS.find('.AtwSecsOnes .AtwCurrentBtn').triggerHandler('click');
		    	else if ( this.fBtn.hasClass('AtwSecOneBtn') )
				{
					if ( key == 9 )
					{
						this.denyTab = false;
						return;
					}
				}
			}
		    else if ( key == 35 ) // END
		    {
		    	if ( this.fBtn.hasClass('AtwMilBtn') || this.fBtn.hasClass('AtwCentBtn') ||
				    this.fBtn.hasClass('AtwDecBtn') || this.fBtn.hasClass('AtwYrBtn') ||
				    this.fBtn.hasClass('AtwEraBtn') )
		    	{
		    		t = this.yDiv.find('.AtwCEBtn');
		    		if ( ! t.length ) 
		    			t = this.yDiv.find('.AtwYr9Btn');
		    		t.triggerHandler('click');
		    	}
		    	else if ( this.fBtn.hasClass('AtwDoMBtn') )
		    	{
		    		t = new Date(this.time.getTime());
					t.setDate(1);
		    		t.setMonth(t.getMonth()+1);
					t.setDate(t.getDate()-1);
			    	if ( event.ctrlKey )
			    		t.setMonth(11);
		    		this.keyDateChange(t);
		    	}
		    	else if ( this.dS )
					this.dS.find('.AtwSec9Btn').triggerHandler('click');
		    	else if ( this.dM )
					this.dM.find('.AtwMin9Btn').triggerHandler('click');
				else if ( this.dH )
					this.dH.find('.AtwHr23Btn').triggerHandler('click');
				else if ( this.dDoM )
					this.dDoM.find('.AtwDoMBtnFilled:last').triggerHandler('click');
				else if ( this.dMo )
					this.dMo.find('.AtwMon12Btn').triggerHandler('click');
				else if ( this.dY )
					this.yAhead.triggerHandler('click');
		    }
		    else if ( key == 36 ) // HOME
		    {
		    	if ( this.fBtn.hasClass('AtwMilBtn') || this.fBtn.hasClass('AtwCentBtn') ||
				    this.fBtn.hasClass('AtwDecBtn') || this.fBtn.hasClass('AtwYrBtn') ||
				    this.fBtn.hasClass('AtwEraBtn') )
				{
		    		this.yDiv.find('.AtwMil0Btn').triggerHandler('click');
		    	}
			    else if ( this.fBtn.hasClass('AtwDoMBtn') )
		    	{
		    		t = new Date(this.time.getTime());
					t.setDate(1);
			    	if ( event.ctrlKey )
			    		t.setMonth(0);
		    		this.keyDateChange(t);
		    	}
				else if ( this.dY )
					this.yCur.triggerHandler('click');
				else if ( this.dMo )
					this.dMo.find('.AtwMon1Btn').triggerHandler('click');
				else if ( this.dDoM )
					this.dDoM.find('.AtwDoMBtnFilled:first').triggerHandler('click');
				else if ( this.dH )
					this.dH.find('.AtwHr0Btn').triggerHandler('click');
		    	else if ( this.dM )
					this.dM.find('.AtwMin00Btn').triggerHandler('click');
		    	else if ( this.dS )
					this.dS.find('.AtwSec00Btn').triggerHandler('click');
		    }
		    else if ( key == 37 ) // left arrow
		    {
		    	if ( this.fBtn.hasClass('AtwDoMBtn') )
		    		this.keyDateChange(new Date(this.time.getTime()-(24*60*60*1000)));
		    	else
		    		this.keyBack();
		    }
		    else if ( key == 38 ) // up arrow
		    {
		    	if ( this.fBtn.hasClass('AtwDoMBtn') )
		    		this.keyDateChange(new Date(this.time.getTime()-(7*24*60*60*1000)));
		    	else
		    		this.keyBack();
		    }
		    else if ( key == 39 ) // right arrow
		    {
		    	if ( this.fBtn.hasClass('AtwDoMBtn') )
		    		this.keyDateChange(new Date(this.time.getTime()+(24*60*60*1000)));
		    	else
		    		this.keyAhead();
		    }
		    else if ( key == 40 ) // down arrow
		    {
		    	if ( this.fBtn.hasClass('AtwDoMBtn') )
		    		this.keyDateChange(new Date(this.time.getTime()+(7*24*60*60*1000)));
		    	else
		    		this.keyAhead();
		    }
		    event.preventDefault();
		
		}, // .key()
	
		//---------------------------------------------------------------------
		//  .keyAhead() is called by #key when a user presses the right or
		//	down arrow.  It moves to the next appropriate button.
		//---------------------------------------------------------------------
		  
		keyAhead: function()
		{
		   	if ( this.fBtn.hasClass('AtwMil9Btn') )
		   		this.yDiv.find('.AtwCent0Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwCent9Btn') )
		   		this.yDiv.find('.AtwDec0Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwDec9Btn') )
		   		this.yDiv.find('.AtwYr0Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwYr9Btn') )
		   		this.yDiv.find('.AtwBCEBtn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwSec9Btn') )
		   		{}
		   	else if ( this.fBtn.hasClass('AtwSec50Btn') )
		   		this.dS.find('.AtwSec0Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwMin9Btn') )
		   	{
		   		if ( this.dS )
		   			this.dS.find('.AtwSec00Btn').triggerHandler('click');
		   	}
		   	else if ( this.fBtn.hasClass('AtwMin50Btn') )
		   		this.dM.find('.AtwMin0Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwHr23Btn') )
		   	{
		   		if ( this.dM )
		   			this.dM.find('.AtwMin00Btn').triggerHandler('click');
		   		else if ( this.dS )
		   			this.dS.find('.AtwSec00Btn').triggerHandler('click');
		   	}
		   	else if ( this.fBtn.hasClass('AtwHr11Btn') )
				this.dH.find('.AtwHr12Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwMon12Btn') )
		   	{
		   		if ( this.dDoM )
		   			this.dDoM.find(__acb).triggerHandler('click');
		   		else if ( this.dH )
		   			this.dH.find('.AtwHr0Btn').triggerHandler('click');
		   		else if ( this.dM )
		   			this.dM.find('.AtwMin00Btn').triggerHandler('click');
		   		else if ( this.dS )
		   			this.dS.find('.AtwSec00Btn').triggerHandler('click');
		   	}
		   	else if ( this.fBtn.hasClass('AtwYrsAheadBtn') )
		   	{
		   		if ( this.dMo )
		   			this.dMo.find('.AtwMon1Btn').triggerHandler('click');
		   		else if ( this.dH )
		   			this.dH.find('.AtwHr0Btn').triggerHandler('click');
		   		else if ( this.dM )
		   			this.dM.find('.AtwMin00Btn').triggerHandler('click');
		   		else if ( this.dS )
		   			this.dS.find('.AtwSec00Btn').triggerHandler('click');
		   	}
		   	else if ( this.fBtn.hasClass('AtwYrCurrentBtn') )
		        this.yNext.triggerHandler('click');
		   	else
				 this.fBtn.next().triggerHandler('click');
		
		}, // .keyAhead()
		
		  
		//---------------------------------------------------------------------
		//  .keyBack() is called by #key when a user presses the left or
		//	up arrow. It moves to the previous appropriate button.
		//---------------------------------------------------------------------
		  
		keyBack: function()
		{
		   	if ( this.fBtn.hasClass('AtwCent0Btn') )
		   		this.yDiv.find('.AtwMil9Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwDec0Btn') )
		   		this.yDiv.find('.AtwCent9Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwYr0Btn') )
		   		this.yDiv.find('.AtwDec9Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwBCEBtn') )
			   		this.yDiv.find('.AtwYr9Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwYrCurrentBtn') )
		        this.yPrior.triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwMon1Btn') )
		   	{
		   		if ( this.dY )
		   			this.yCur.triggerHandler('click');
		   	}
		   	else if ( this.fBtn.hasClass('AtwHr0Btn') )
		   	{
		   		if ( this.dDoM )
		   			this.dDoM.find(__acb).triggerHandler('click');
		   		else if ( this.dMo )
		   			this.dMo.find('.AtwMon12Btn').triggerHandler('click');
		   		else if ( this.dY )
		   			this.yNext.triggerHandler('click');
		   	}
		   	else if ( this.fBtn.hasClass('AtwHr12Btn') )
		   		 this.dH.find('.AtwHr11Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwMin00Btn') )
		   	{
		   		if ( this.dH )
		   			this.dH.find('.AtwHr23Btn').triggerHandler('click');
		   		else if ( this.dDoM )
		   			this.dDoM.find(__acb).triggerHandler('click');
		   		else if ( this.dMo )
		   			this.dMo.find('.AtwMon12Btn').triggerHandler('click');
		   		else if ( this.dY )
		   			this.yNext.triggerHandler('click');
		   	}
		   	else if ( this.fBtn.hasClass('AtwMin0Btn') )
		   		 this.dM.find('.AtwMin50Btn').triggerHandler('click');
		   	else if ( this.fBtn.hasClass('AtwSec00Btn') )
		   	{
		   		if ( this.dM )
		   			this.dM.find('.AtwMin9Btn').triggerHandler('click');
		   		else if ( this.dH )
		   			this.dH.find('.AtwHr23Btn').triggerHandler('click');
		   		else if ( this.dDoM )
		   			this.dDoM.find(__acb).triggerHandler('click');
		   		else if ( this.dMo )
		   			this.dMo.find('.AtwMon12Btn').triggerHandler('click');
		   		else if ( this.dY )
		   			this.yNext.triggerHandler('click');
		   	}
		   	else if ( this.fBtn.hasClass('AtwSec0Btn') )
		   		 this.dS.find('.AtwSec50Btn').triggerHandler('click');
		   	else
				 this.fBtn.prev().triggerHandler('click');
		
		}, // .keyBack()
		
		//---------------------------------------------------------------------
		//  .keyDateChange() is called by #key when an direction key
		//	(arrows/page/etc) is pressed while the Day-of-Month calendar has
		//	focus. The current day is adjusted accordingly.
		//---------------------------------------------------------------------
		  
		keyDateChange: function( newDate )
		{
			if ( this.fBtn.hasClass('AtwDoMBtn') )
			{
				if ( this.set(newDate) )
				{
					this.upd(null);
					this.setFocus( this.dDoM.find(__acb) );
				}
			}
		},
		  
		//---------------------------------------------------------------------
		//  .newHour() is called when a user clicks an hour value.
		//  It changes the date and updates the text field.
		//---------------------------------------------------------------------
		
		newHour: function( event )
		{
		    var h;
		    var t;
		    var elem = $(event.target);
		    if ( ! this.twelveHr )
		      h = Number(elem.text());
		    else
		    {
		      var str = elem.text();
		      t = str.indexOf('a');
		      if ( t < 0 )
		      {
		        t = Number(str.substr(0,str.indexOf('p')));
		        h = ( (t==12) ? 12 : (t+12) );
		      }
		      else
		      {
		        t = Number(str.substr(0,t));
		        h = ( (t==12) ? 0 : t );
		      }
		    }
		    t = new Date(this.time.getTime());
		    t.setHours(h);
		    if ( this.set(t) )
		      this.upd(elem);
		    
		}, // .newHour()
		
		//---------------------------------------------------------------------
		//  .newYear() is called when a user clicks a year (or one of the
		//	"arrows") to shift the year.  It changes the date and updates the
		//	text field.
		//---------------------------------------------------------------------
		
		newYear: function( event )
		{
		    var elem = $(event.target);
		    var txt = elem.text();
		    if ( ( txt == '<' ) || ( txt == '&lt;' ) )
		      this.askYear(event);
		    else if ( ( txt == '>' ) || ( txt == '&gt;' ) )
		      this.askYear(event);
		    else
		    {
		      var t = new Date(this.time.getTime());
		      t.setFullYear(Number(txt));
		      if ( this.set(t) )
		        this.upd(this.yCur);
		    }
		},
		
		//---------------------------------------------------------------------
		//  .newYPos() is called internally whenever a user clicks a year
		//  selection value.  It changes the date and updates the text field.
		//---------------------------------------------------------------------
		
		newYPos: function( event )
		{
		    var elem = $(event.target);
		    
		    var era = 1;
		    var year = this.time.getFullYear();
		    if ( year < 0 )
		    {
		      era = (-1);
		      year = 0 - year;
		    }
		    year = AnyTime.pad( year, 4 );
		    if ( elem.hasClass('AtwMilBtn') )
		      year = elem.html() + year.substring(1,4);
		    else if ( elem.hasClass('AtwCentBtn') )
		      year = year.substring(0,1) + elem.html() + year.substring(2,4);
		    else if ( elem.hasClass('AtwDecBtn') )
		      year = year.substring(0,2) + elem.html() + year.substring(3,4);
		    else
		      year = year.substring(0,3) + elem.html();
		    if ( year == '0000' )
		      year = 1;
		    var t = new Date(this.time.getTime());
		    t.setFullYear( era * year );
		    if ( this.set(t) )
		      this.updYDiv(elem);
		
		}, // .newYPos()
		
		//---------------------------------------------------------------------
		//  .onReady() initializes the widget after the page has loaded and,
		//  if IE6, after the iframe has been created.
		//---------------------------------------------------------------------
		
		onReady: function()
		{
			this.lostFocus = true;
			if ( ! this.pop )
				this.upd(null);
			else 
			{
				if ( this.div.parent() != document.body )
					this.div.appendTo( document.body );
			}
		},
		
		//---------------------------------------------------------------------
		//  .pos() positions the widget, such as when it is displayed or
		//	when the window is resized.
		//---------------------------------------------------------------------
		
		pos: function(event) // note: event is ignored but this is a handler
		{
		    if ( this.pop )
		    {
		      var off = this.inp.offset();
		      var bodyWidth = $(document.body).outerWidth(true);
		      var widgetWidth = this.div.outerWidth(true);
		      var left = off.left;
		      if ( left + widgetWidth > bodyWidth - 20 )
		        left = bodyWidth - ( widgetWidth + 20 );
		      var top = off.top - this.div.outerHeight(true);
		      if ( top < 0 )
		        top = off.top + this.inp.outerHeight(true);
		      this.div.css( { top: String(top)+'px', left: String(left<0?0:left)+'px' } );
		    }
		
		    if ( this.yDiv )
		    {
		      var wOff = this.div.offset();
		      var yOff = this.yLab.offset();
		      if ( this.div.css('position') == 'absolute' )
		      {
		    	  yOff.top -= wOff.top;
		          yOff.left = yOff.left - wOff.left;
		    	  wOff = { top: 0, left: 0 };
		      }
		      yOff.left += ( (this.yLab.outerWidth(true)-this.yDiv.outerWidth(true)) / 2 );
		      this.cloak.css( { 
		      	top: wOff.top+'px',
		      	left: wOff.left+'px',
		      	height: String(this.div.outerHeight(true)-2)+'px',
		    	width: String(this.div.outerWidth(!$.browser.safari)-2)+'px'
		    	} );
		      this.yDiv.css( { top: yOff.top+'px', left: yOff.left+'px' } ) ;
		    }
		
		}, // .pos()
		
		//---------------------------------------------------------------------
		//  .set() changes the current time.  It returns true if the new
		//	time is within the allowed range (if any).
		//---------------------------------------------------------------------
		
		set: function(newTime)
		{
		    var t = newTime.getTime();
		    if ( this.earliest && ( t < this.earliest ) )
		      return false;
		    if ( this.latest && ( t > this.latest ) )
		      return false;
		    this.time = newTime;
		    return true;
		    
		},
		  
		//---------------------------------------------------------------------
		//  .showCal() displays the widget and sets the focus psuedo-
		//	element. The current value in the input field is used to initialize
		//	the widget.
		//---------------------------------------------------------------------
		
		showCal: function(event)
		{
			try
		    {
		      this.time = this.conv.parse(this.inp.get(0).value);
		    }
		    catch ( e )
		    {
		      this.time = new Date();
		    }
		
		    this.upd(null);
		    
		    fBtn = null;
		    var cb = '.AtwCurrentBtn:first';
		    if ( this.dDoM )
		    	fBtn = this.dDoM.find(cb);
			else if ( this.yCur )
				fBtn = this.yCur;
			else if ( this.dMo )
				fBtn = this.dMo.find(cb);
			else if ( this.dH )
				fBtn = this.dH.find(cb);
			else if ( this.dM )
				fBtn = this.dM.find(cb);
			else if ( this.dS )
				fBtn = this.dS.find(cb);
		
		    this.setFocus(fBtn);
		    this.pos(event);
		
			//  IE6 doesn't float popups over <select> elements unless an
		    //	<iframe> is inserted between them!  So after the widget is
		    //	made visible, move the <iframe> behind it.
		    
		    if ( this.pop && __iframe )
		        setTimeout(
		        	function()
					{
						var pos = _this.div.offset();
						__iframe.css( {
						    height: String(_this.div.outerHeight(true)) + 'px',
						    left: String(pos.left) + 'px',
						    position: 'absolute',
						    top: String(pos.top) + 'px',
						    width: String(_this.div.outerWidth(true)) + 'px'
						    } );
						__iframe.show();
					}, 300 );
		
		}, // .showCal()
		
		//---------------------------------------------------------------------
		//  .upd() updates the widget's appearance.  It is called after
		//	most events to make the widget reflect the currently-selected
		//	values. fBtn is the psuedo-button to be given focus.
		//---------------------------------------------------------------------
		
		upd: function(fBtn)
		{
		    //  Update year.
		
		    var current = this.time.getFullYear();
		    if ( this.yPrior )
		      this.yPrior.text(AnyTime.pad((current==1)?(-1):(current-1),4));
		    if ( this.yCur )
		      this.yCur.text(AnyTime.pad(current,4));
		    if ( this.yNext )
		      this.yNext.text(AnyTime.pad((current==-1)?1:(current+1),4));
		
		    //  Update month.
		
		    var i = 0;
		    current = this.time.getMonth();
		    $('#'+this.id+' .AtwMonBtn').each(
		      function()
		      {
		        _this.updCur( $(this), i == current );
		        i++;
		      } );
		
		    //  Update days.
		
		    current = this.time.getDate();
		    var currentMonth = this.time.getMonth();
		    var dom = new Date(this.time.getTime());
		    dom.setDate(1);
		    var dow1 = dom.getDay();
		    if ( this.fDOW > dow1 )
		      dow1 += 7;
		    var wom = 0, dow=0;
		    $('#'+this.id+' .AtwWk').each(
		      function()
		      {
		        dow = _this.fDOW;
		        $(this).children().each(
		          function()
		          {
		        	  if ( dow - _this.fDOW < 7 )
		        	  {
		        		  var td = $(this);
				          if ( ((wom==0)&&(dow<dow1)) || (dom.getMonth()!=currentMonth) )
				          {
				            td.html('&nbsp;');
				            td.removeClass('AtwDoMBtnFilled AtwCurrentBtn ui-state-default ui-state-highlight');
				            td.addClass('AtwDoMBtnEmpty');
				            if ( wom ) // not first week
				            {
				            	if ( ( dom.getDate() == 1 ) && ( dow != 1 ) )
				            		td.addClass('AtwDoMBtnEmptyAfterFilled');
				            	else
				            		td.removeClass('AtwDoMBtnEmptyAfterFilled');
				            	if ( dom.getDate() <= 7 )
				            		td.addClass('AtwDoMBtnEmptyBelowFilled');
				            	else
				            		td.removeClass('AtwDoMBtnEmptyBelowFilled');
				                dom.setDate(dom.getDate()+1);
				            }
				            else // first week
				            {
				            	td.addClass('AtwDoMBtnEmptyAboveFilled');
				            	if ( dow == dow1 - 1 )
				            		td.addClass('AtwDoMBtnEmptyBeforeFilled');
				            	else
				            		td.removeClass('AtwDoMBtnEmptyBeforeFilled');
				            }
				            td.addClass('ui-state-default ui-state-disabled');
				          }
				          else
				          {
				            td.text(dom.getDate());
				            td.removeClass('AtwDoMBtnEmpty AtwDoMBtnEmptyAboveFilled AtwDoMBtnEmptyBeforeFilled '+
				            				'AtwDoMBtnEmptyAfterFilled AtwDoMBtnEmptyBelowFilled ' +
				            				'ui-state-default ui-state-disabled');
				            td.addClass('AtwDoMBtnFilled ui-state-default');
				            _this.updCur( td, dom.getDate() == current );
				            dom.setDate(dom.getDate()+1);
				          }
		        	  }
		              dow++;
		          } );
		          wom++;
		      } );
		
		    //  Update hour.
		
		    var not12 = ! this.twelveHr;
		    var h = this.time.getHours();
		    $('#'+this.id+' .AtwHrBtn').each(
		      function()
		      {
		        _this.updCur( $(this),
		        	( not12 && (Number(this.innerHTML)==h) ) || 
		        		( this.innerHTML == String((h==0)?12:((h<=12)?h:(h-12)))+((h<12)?'am':'pm') ) );
		      } );
		
		    //  Update minute.
		
		    var mi = this.time.getMinutes();
		    var tens = Math.floor(mi/10);
		    var ones = mi % 10;
		    $('#'+this.id+' .AtwMinTenBtn').each(
		      function()
		      {
		        _this.updCur( $(this), this.innerHTML == String(tens) );
		      } );
		    $('#'+this.id+' .AtwMinOneBtn').each(
		      function()
		      {
		        _this.updCur( $(this), this.innerHTML == String(ones) );
		      } );
		
		    //  Update second.
		
		    var mi = this.time.getSeconds();
		    var tens = Math.floor(mi/10);
		    var ones = mi % 10;
		    $('#'+this.id+' .AtwSecTenBtn').each(
		      function()
		      {
		        _this.updCur( $(this), this.innerHTML == String(tens) );
		      } );
		    $('#'+this.id+' .AtwSecOneBtn').each(
		      function()
		      {
		        _this.updCur( $(this), this.innerHTML == String(ones) );
		      } );
		
		
		    //	Set the focus element, then size the widget according to its
		    //	components, show the changes, and invoke Ajax if desired.
		    
		    if ( fBtn )
		    	this.setFocus(fBtn);
		
		    this.inp.get(0).value = this.conv.format( this.time );
		    this.div.show();
		
		    var d, totH = 0, totW = 0, dYW = 0, dMoW = 0, dDoMW = 0;
		    if ( this.dY )
		    {
		    	totW = dYW = this.dY.outerWidth(true);
				totH = this.yLab.AtwHeight(true) + this.dY.AtwHeight(true);
		    }
		    if ( this.dMo )
		    {
		    	dMoW = this.dMo.outerWidth(true);
		    	if ( dMoW > totW )
		    		totW = dMoW;
		    	totH += this.hMo.AtwHeight(true) + this.dMo.AtwHeight(true);
		    }
		    if ( this.dDoM )
		    {
			    dDoMW = this.dDoM.outerWidth(true);
			    if ( dDoMW > totW )
			    	totW = dDoMW;
			    if ( __msie6 || __msie7 )
			    {
			    	if ( dMoW > dDoMW )
			    		this.dDoM.css('width',String(dMoW)+'px');
			    	else if ( dYW > dDoMW )
			    		this.dDoM.css('width',String(dYW)+'px');
			    }
			    totH += this.hDoM.AtwHeight(true) + this.dDoM.AtwHeight(true);
		    }
		    if ( this.dD )
		    {
		    	this.dD.css( { width:String(totW)+'px', height:String(totH)+'px' } );
		        totW += this.dMinW;
		        totH += this.dMinH;
		    }
		
		    var w = 0, h = 0, timeH = 0, timeW = 0;
		    if ( this.dH )
		    {
		    	w = this.dH.outerWidth(true);
		    	timeW += w + 1;
		    	h = this.dH.AtwHeight(true);
		    	if ( h > timeH )
		    		timeH = h;
		    }
		    if ( this.dM )
		    {
		        w = this.dM.outerWidth(true);
		        timeW += w + 1;
		        h = this.dM.AtwHeight(true);
		        if ( h > timeH )
		        	timeH = h;
		    }
		    if ( this.dS )
		    {
		        w = this.dS.outerWidth(true);
		        timeW += w + 1;
		        h = this.dS.AtwHeight(true);
		        if ( h > timeH )
		        	timeH = h;
		    }
		    if ( this.dT )
		    {
		    	this.dT.css( { width:String(timeW)+'px', height:String(timeH)+'px' } );
		    	timeW += this.tMinW + 1;
		        timeH += this.tMinH;
		    	totW += timeW;
		    	if ( timeH > totH )
		    		totH = timeH;
		    }
		    	
		    this.dB.css({height:String(totH)+'px',width:String(totW)+'px'});
		
		    totH += this.bMinH;
		    totW += this.bMinW;
		    totH += this.hTitle.AtwHeight(true) + this.wMinH;
		    totW += this.wMinW;
		    if ( this.hTitle.outerWidth(true) > totW )
		        totW = this.hTitle.outerWidth(true); // IE quirk
		    this.div.css({height:String(totH)+'px',width:String(totW)+'px'});
		
		    if ( ! this.pop )
		      this.ajax();
		
		}, // .upd()
		
		//---------------------------------------------------------------------
		//  .updCur() updates the specified psuedo-button "btn" based on
		//	whether parameter "cur" says that it reflects the "current" value.
		//---------------------------------------------------------------------
		
		updCur: function(btn,cur)
		{
		    if ( cur )
		    {
			  btn.removeClass('ui-state-default ui-state-highlight');
		      btn.addClass('AtwCurrentBtn ui-state-default ui-state-highlight');
		    }
		    else
		      btn.removeClass('AtwCurrentBtn ui-state-highlight');
		},
		
		//---------------------------------------------------------------------
		//  .updYDiv() updates the year selector's appearance.  It is
		//	called after most events to make the widget reflect the currently-
		//	selected values. fBtn is the psuedo-button to be given focus.
		//---------------------------------------------------------------------
		
		updYDiv: function(fBtn)
		{
			var era = 1;
		    var yearValue = this.time.getFullYear();
		    if ( yearValue < 0 )
		    {
		      era = (-1);
		      yearValue = 0 - yearValue;
		    }
		    yearValue = AnyTime.pad( yearValue, 4 );
		    this.yDiv.find('.AtwMilBtn').each(
		      function()
		      {
		        _this.updCur( $(this), this.innerHTML == yearValue.substring(0,1) );
		      } );
		    this.yDiv.find('.AtwCentBtn').each(
		      function()
		      {
		        _this.updCur( $(this), this.innerHTML == yearValue.substring(1,2) );
		      } );
		    this.yDiv.find('.AtwDecBtn').each(
		      function()
		      {
		        _this.updCur( $(this), this.innerHTML == yearValue.substring(2,3) );
		      } );
		    this.yDiv.find('.AtwYrBtn').each(
		      function()
		      {
		        _this.updCur( $(this), this.innerHTML == yearValue.substring(3) );
		      } );
		    this.yDiv.find('.AtwBCEBtn').each(
		      function()
		      {
		        _this.updCur( $(this), era < 0 );
		      } );
		    this.yDiv.find('.AtwCEBtn').each(
		      function()
		      {
		        _this.updCur( $(this), era > 0 );
		      } );
		
		    //  Show change
		
		    this.inp.get(0).value = this.conv.format( this.time );
		    this.upd(fBtn);
		
		} // .updYDiv()

	}; // __widgets[id] = ...
	__widgets[id].initialize(id);
	
} // AnyTime.widget()

}; // return

})(); // var AnyTime = function(...)

//
//  END OF FILE
//
