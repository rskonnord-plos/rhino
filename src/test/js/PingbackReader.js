/*
 * Copyright (c) 2013 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var DOI_SCHEME = 'info:doi/';
var SERVER_ROOT = 'http://localhost:8080/';

function main() {
  $('#jsWarning').hide();
  $.ajax({
    url: SERVER_ROOT + 'articles?pingbacks',
    dataType: 'jsonp',
    success: function (data, textStatus, jqXHR) {
      populateArticleTable(data);
    },
    error: function (jqXHR, textStatus, errorThrown) {
      alert(textStatus);
    },
    complete: function (jqXHR, textStatus) {
    }
  });
}

function populateArticleTable(pingbacksByArticle) {
  var articleTable = $('table.articles');
  $.each(pingbacksByArticle, function (index, article) {
    var articleRow = $('<tr/>');
    var fetchCell = $('<td/>').addClass('fetch').attr('colspan', 5);
    var fetchRow = $('<tr/>').append(fetchCell).hide();
    articleRow
      .append($('<td/>').text(article.doi))
      .append($('<td/>').html($('<a/>').attr('href', article.articleUrl).text(article.title)))
      .append($('<td/>').text(article.pingbackCount))
      .append($('<td/>').text(article.mostRecentPingback))
      .append($('<td/>').append(makeFetchButton(article, articleRow, fetchRow)))
    articleTable.append(articleRow).append(fetchRow);
  });
}

function makeFetchButton(article, articleRow, fetchRow) {
  var button = $('<button/>').text('Fetch');
  button.click(function () {
    button.attr('disabled', true);
    fetchRow.show().find('.fetch').html(fetchPingbacks(article));
  });
  return button;
}

function doiAsIdentifier(doi) {
  return (doi.substr(0, DOI_SCHEME.length) === DOI_SCHEME) ? doi.substr(DOI_SCHEME.length) : doi;
}

function fetchPingbacks(article) {
  var headerText = "Pingbacks for \"" + article.title + "\"";
  var fetchBox = $('<span/>');
  fetchBox.append($('<h3/>').text(headerText));
  $.ajax({
    url: SERVER_ROOT + 'articles/' + doiAsIdentifier(article.doi) + '?pingbacks',
    dataType: 'jsonp',
    success: function (data, textStatus, jqXHR) {
      populatePingbacks(fetchBox, data);
    },
    error: function (jqXHR, textStatus, errorThrown) {
      alert(textStatus);
    },
    complete: function (jqXHR, textStatus) {
    }
  });
  return fetchBox;
}

function populatePingbacks(box, pingbacksList) {
  var table = $('table.pingbacks.prototype').clone().removeClass('prototype').show();
  $.each(pingbacksList, function (index, pingback) {
    var row = $('<tr/>')
      .append($('<td/>').text(pingback.title))
      .append($('<td/>').html($('<a/>').attr('href', pingback.url).text(pingback.url)))
      .append($('<td/>').text(pingback.created))
    table.append(row);
  });
  box.append(table);
  return box;
}

$(document).ready(main);