/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the 'License'); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

describe('The graph refresher', function () {
    var graphId = 1242;

    function cleanDates() {
        $('#title_from_date_' + graphId).html('');
        $('#title_to_date_' + graphId).html('');
    }

    it('should update the start and end date in the graph title', function() {
        $('html,body')
            .append($('<span></span>').attr('id', 'title_from_date_' + graphId))
            .append($('<span></span>').attr('id', 'title_to_date_' + graphId));
        $('#title_from_date_' + graphId).html('GARBAGE');
        $('#title_to_date_' + graphId).html('GARBAGE');
        verify($('#title_from_date_' + graphId), '<span id="title_from_date_1242">GARBAGE</span>');
        verify($('#title_to_date_' + graphId), '<span id="title_to_date_1242">GARBAGE</span>');

        // A refresh with an empty graph item should clear the elements
        cleanDates();
        refreshStartAndEndTime(undefined);
        verify($('#title_from_date_' + graphId), '<span id="title_from_date_1242"></span>');
        verify($('#title_to_date_' + graphId), '<span id="title_to_date_1242"></span>');

        var startDate = new Date('Wed, 11 Apr 2012 12:20 GMT');
        var endDate = new Date('Thu, 12 Apr 2012 12:42 GMT');

        // A refresh with invalid dates should clear the elements
        cleanDates();
        refreshStartAndEndTime({graphId: graphId, startDate: 'someTime', endDate: endDate});
        verify($('#title_from_date_' + graphId), '<span id="title_from_date_1242"><h6></h6></span>');
        verify($('#title_to_date_' + graphId), '<span id="title_to_date_1242"><h6>2012-04-12T12:42:00Z</h6></span>');

        cleanDates();
        refreshStartAndEndTime({graphId: graphId, startDate: startDate, endDate: 'someOtherTime'});
        verify($('#title_from_date_' + graphId), '<span id="title_from_date_1242"><h6>2012-04-11T12:20:00Z</h6></span>');
        verify($('#title_to_date_' + graphId), '<span id="title_to_date_1242"><h6></h6></span>');

        cleanDates();
        refreshStartAndEndTime({graphId: graphId, startDate: 'someTime', endDate: 'someOtherTime'});
        verify($('#title_from_date_' + graphId), '<span id="title_from_date_1242"><h6></h6></span>');
        verify($('#title_to_date_' + graphId), '<span id="title_to_date_1242"><h6></h6></span>');

        cleanDates();
        refreshStartAndEndTime({graphId: graphId, startDate: startDate, endDate: endDate});
        verify($('#title_from_date_' + graphId), '<span id="title_from_date_1242"><h6>2012-04-11T12:20:00Z</h6></span>');
        verify($('#title_to_date_' + graphId), '<span id="title_to_date_1242"><h6>2012-04-12T12:42:00Z</h6></span>');
    });
});