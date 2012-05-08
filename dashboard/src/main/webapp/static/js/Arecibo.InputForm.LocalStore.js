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

Arecibo.namespace('Arecibo.InputForm.LocalStore');

Arecibo.InputForm.LocalStore.get = function(key) {
    try {
        return localStorage.getItem(key);
    } catch (e) { /* Ignore quota issues, non supported Browsers, etc. */ }
};

Arecibo.InputForm.LocalStore.set = function(key, value) {
    try {
        localStorage.setItem(key, value);
    } catch (e) { /* Ignore quota issues, non supported Browsers, etc. */ }
};

Arecibo.InputForm.LocalStore.getHosts = function(hosts) {
    return JSON.parse(Arecibo.InputForm.LocalStore.get('arecibo_hosts'))
};

Arecibo.InputForm.LocalStore.setHosts = function(hosts) {
    Arecibo.InputForm.LocalStore.set('arecibo_hosts', JSON.stringify(hosts));
};

Arecibo.InputForm.LocalStore.getHostsEtag = function(hosts) {
    return Arecibo.InputForm.LocalStore.get('arecibo_hosts_etag');
};

Arecibo.InputForm.LocalStore.setHostsEtag = function(etag) {
    Arecibo.InputForm.LocalStore.set('arecibo_hosts_etag', etag);
};

Arecibo.InputForm.LocalStore.getLatestHostsSelected = function(hosts) {
    var hosts = JSON.parse(Arecibo.InputForm.LocalStore.get('arecibo_latest_hosts'));
    if (!hosts) {
        return Set.makeSet();
    } else {
        return hosts;
    }
};

Arecibo.InputForm.LocalStore.setLatestHostsSelected = function(hosts) {
    Arecibo.InputForm.LocalStore.set('arecibo_latest_hosts', JSON.stringify(hosts));
};

Arecibo.InputForm.LocalStore.getSampleKinds = function(hosts) {
    return JSON.parse(Arecibo.InputForm.LocalStore.get('arecibo_sample_kinds'));
};

Arecibo.InputForm.LocalStore.setSampleKinds = function(hosts) {
    Arecibo.InputForm.LocalStore.set('arecibo_sample_kinds', JSON.stringify(hosts));
};

Arecibo.InputForm.LocalStore.getSampleKindsEtag = function(hosts) {
    return Arecibo.InputForm.LocalStore.get('arecibo_sample_kinds_etag');
};

Arecibo.InputForm.LocalStore.setSampleKindsEtag = function(etag) {
    Arecibo.InputForm.LocalStore.set('arecibo_sample_kinds_etag', etag);
};

Arecibo.InputForm.LocalStore.getLatestSampleKindsSelected = function(hosts) {
    var sampleKinds = JSON.parse(Arecibo.InputForm.LocalStore.get('arecibo_latest_sample_kinds'));
    if (!sampleKinds) {
        return Set.makeSet();
    } else {
        return sampleKinds;
    }
};

Arecibo.InputForm.LocalStore.setLatestSampleKindsSelected = function(hosts) {
    Arecibo.InputForm.LocalStore.set('arecibo_latest_sample_kinds', JSON.stringify(hosts));
};