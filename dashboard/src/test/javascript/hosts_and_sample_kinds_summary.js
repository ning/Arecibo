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

describe('The Arecibo selection page', function () {
    it('should update the summary box for hosts', function() {
        $('html,body').append($('<ul></ul>').attr('id', 'hosts_summary_list'));
        $('#hosts_summary_list').html('GARBAGE');
        verify($('#hosts_summary_list'), '<ul id="hosts_summary_list">GARBAGE</ul>')

        // An update with an empty list of hosts should clear the list
        updateHostsSelectedSummary([]);
        verify($('#hosts_summary_list'), '<ul id="hosts_summary_list"></ul>')

        updateHostsSelectedSummary([{hostName: 'A'}]);
        verify($('#hosts_summary_list'), '<ul id="hosts_summary_list"><li>A</li></ul>')

        updateHostsSelectedSummary([{hostName: 'A'}, {hostName: 'B'}]);
        verify($('#hosts_summary_list'), '<ul id="hosts_summary_list"><li>A</li><li>B</li></ul>')
    });

    it('should update the summary box for sample kinds', function() {
        $('html,body').append($('<ul></ul>').attr('id', 'sample_kinds_summary_list'));
        $('#sample_kinds_summary_list').html('GARBAGE');
        verify($('#sample_kinds_summary_list'), '<ul id="sample_kinds_summary_list">GARBAGE</ul>')

        // An update with an empty list of sample kinds should clear the list
        updateSampleKindsSelectedSummary([]);
        verify($('#sample_kinds_summary_list'), '<ul id="sample_kinds_summary_list"></ul>')

        updateSampleKindsSelectedSummary([{sampleCategory: 'A', sampleKind: 'B'}]);
        verify($('#sample_kinds_summary_list'), '<ul id="sample_kinds_summary_list"><li>A::B</li></ul>')

        updateSampleKindsSelectedSummary([{sampleCategory: 'A', sampleKind: 'B'}, {sampleCategory: 'C', sampleKind: 'D'}]);
        verify($('#sample_kinds_summary_list'), '<ul id="sample_kinds_summary_list"><li>A::B</li><li>C::D</li></ul>')
    });
});