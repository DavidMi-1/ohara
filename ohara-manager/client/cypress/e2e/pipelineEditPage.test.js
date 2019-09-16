/*
 * Copyright 2019 is-land
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

import * as URLS from '../../src/constants/urls';
import { CONNECTOR_TYPES } from '../../src/constants/pipelines';
import * as generate from '../../src/utils/generate';

describe('PipelineEditPage', () => {
  before(() => {
    cy.removeWorkers();
    cy.addWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('POST', 'api/pipelines').as('postPipeline');
    cy.route('GET', 'api/pipelines/*').as('getPipeline');
    cy.route('PUT', 'api/pipelines/*').as('putPipeline');
    cy.route('GET', 'api/topics').as('getTopics');
    cy.route('GET', '/api/connectors/*').as('getConnector');
    cy.route('PUT', '/api/connectors/*').as('putConnector');
    cy.route('POST', '/api/connectors/*').as('postConnector');
    cy.route('PUT', '/api/validate/*').as('putValidateConnector');

    const pipelineName = generate.serviceName({ prefix: 'pi', length: 3 });
    cy.addTopic();
    cy.visit(URLS.PIPELINES)
      .getByTestId('new-pipeline')
      .click()
      .getByTestId('pipeline-name-input')
      .type(pipelineName)
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('WORKER_NAME')}]`)
      .click()
      .getByText('ADD')
      .click()
      .wait('@postPipeline')
      .wait('@getPipeline');
  });

  it('adds a topic into pipeline graph and removes it later', () => {
    // Add the topic
    cy.wait('@getTopics')
      .getByTestId('toolbar-topics')
      .click()
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .getByText('ADD')
      .click()
      .wait('@putPipeline')
      .getByText(Cypress.env('TOPIC_NAME'))
      .should('be.exist');

    // Remove the topic
    cy.getByText(Cypress.env('TOPIC_NAME'))
      .click({ force: true })
      .getByTestId('delete-button')
      .click()
      .getByText('DELETE')
      .click()
      .getByText(`Successfully deleted the topic: ${Cypress.env('TOPIC_NAME')}`)
      .wait('@getPipeline')
      .wait(500) // wait here, so the local state is up-to-date with the API response
      .queryByText(Cypress.env('TOPIC_NAME'), { timeout: 500 })
      .should('not.be.exist');
  });

  it('should prevent user from deleting a topic that has connection', () => {
    // Add topic
    cy.wait('@getTopics')
      .getByTestId('toolbar-topics')
      .click()
      .getByTestId('topic-select')
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .getByText('ADD')
      .click()
      .wait('@putPipeline');

    // Add ftp source connector
    const connectorName = generate.serviceName({ prefix: 'connector' });

    cy.getByTestId('toolbar-sources')
      .click()
      .getByText(CONNECTOR_TYPES.jdbcSource)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('myconnector')
      .type(connectorName)
      .getByTestId('new-connector-dialog')
      .within(() => {
        cy.getByText('ADD').click();
      })
      .wait('@putPipeline');

    // Set the connection between them
    cy.getByText(connectorName)
      .click({ force: true })
      .wait('@getConnector')
      .getByText('core')
      .click({ force: true })
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline');

    // Try to remove the topic
    cy.getByTestId('pipeline-graph')
      .within(() => {
        cy.getByText(Cypress.env('TOPIC_NAME')).click({ force: true });
      })
      .getByTestId('delete-button')
      .click()
      .getByTestId('delete-dialog')
      .within(() => {
        cy.getByText('DELETE').click();
      });

    cy.getByText('You cannot delete the topic while it has any connection');
    // The topic should still be there
    cy.getByTestId('pipeline-graph').within(() =>
      cy.getByText(Cypress.env('TOPIC_NAME')),
    );
  });

  it('adds and deletes all connectors', () => {
    const connectors = [
      {
        type: CONNECTOR_TYPES.ftpSource,
        nodeType: 'FtpSource',
        toolbarTestId: 'toolbar-sources',
        connectorName: generate.serviceName({ prefix: 'connector' }),
      },
      {
        type: CONNECTOR_TYPES.jdbcSource,
        nodeType: 'JDBCSourceConnector',
        toolbarTestId: 'toolbar-sources',
        connectorName: generate.serviceName({ prefix: 'connector' }),
      },
      {
        type: CONNECTOR_TYPES.ftpSink,
        nodeType: 'FtpSink',
        toolbarTestId: 'toolbar-sinks',
        connectorName: generate.serviceName({ prefix: 'connector' }),
      },
      {
        type: CONNECTOR_TYPES.hdfsSink,
        nodeType: 'HDFSSink',
        toolbarTestId: 'toolbar-sinks',
        connectorName: generate.serviceName({ prefix: 'connector' }),
      },
    ];

    // Add connector to the graph
    cy.wrap(connectors).each(connector => {
      const { toolbarTestId, type, nodeType, connectorName } = connector;
      cy.getByTestId(toolbarTestId)
        .click()
        .getByText(type)
        .click()
        .getByText('ADD')
        .click()
        .getByPlaceholderText('myconnector')
        .type(connectorName)
        .getByTestId('new-connector-dialog')
        .within(() => {
          cy.getByText('ADD').click();
        });

      cy.wait(['@postConnector', '@putPipeline'])
        .getAllByText(connectorName)
        .should('have.length', 1)
        .get('.node-type')
        .should('contain', nodeType);
    });

    // Remove connectors from the graph
    cy.wrap(connectors).each(connector => {
      const { connectorName } = connector;

      cy.getByText(connectorName)
        .click({ force: true })
        .getByTestId('delete-button')
        .click()
        .getByText('DELETE')
        .click()
        .getByText(`Successfully deleted the connector: ${connectorName}`)
        .wait('@getPipeline')
        .wait(500) // wait here, so the local state is up-to-date with the API response
        .queryByText(connectorName, { timeout: 500 })
        .should('not.be.exist');
    });
  });

  it('saves and removes a connector even after page refresh', () => {
    const connectorName = generate.serviceName({ prefix: 'connector' });
    cy.getByTestId('toolbar-sources')
      .click()
      .getByText(CONNECTOR_TYPES.jdbcSource)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('myconnector')
      .type(connectorName)
      .getByTestId('new-connector-dialog')
      .within(() => {
        cy.getByText('ADD').click();
      })
      .getByText(connectorName)
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'JDBCSourceConnector')
      .wait(3000)
      .reload()
      .getByText(connectorName)
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'JDBCSourceConnector');

    cy.getByText(connectorName)
      .click({ force: true })
      .getByTestId('delete-button')
      .click()
      .getByText('DELETE')
      .click()
      .getByText(`Successfully deleted the connector: ${connectorName}`)
      .should('be.exist')
      .wait(3000)
      .reload()
      .queryByText(connectorName, { timeout: 500 })
      .should('not.be.exist')
      .get('.node-type')
      .should('not.be.exist');
  });

  it('connects Ftp soure -> Topic -> Ftp sink', () => {
    cy.getByTestId('toolbar-sinks')
      .click()
      .getByText(CONNECTOR_TYPES.ftpSink)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .getByTestId('new-connector-dialog')
      .within(() => {
        cy.getByText('ADD').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-sources')
      .click()
      .getByText(CONNECTOR_TYPES.ftpSource)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .getByTestId('new-connector-dialog')
      .within(() => {
        cy.getByText('ADD').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-topics')
      .click()
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .getByText('ADD')
      .click()
      .wait('@putPipeline');

    cy.getByText('FtpSink')
      .click({ force: true })
      .getByText('core')
      .click()
      .wait('@getConnector')
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 1);

    cy.wait(1000); // Temp workaround, need this to make sure Cypress can get the right element to work on

    cy.getByText('FtpSource')
      .click({ force: true })
      .wait('@getConnector')
      .getByText('core')
      .click({ force: true })
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 2);
  });

  it('connects Jdbc source -> Topic -> Hdfs sink together', () => {
    cy.getByTestId('toolbar-sinks')
      .click()
      .getByText(CONNECTOR_TYPES.hdfsSink)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .getByTestId('new-connector-dialog')
      .within(() => {
        cy.getByText('ADD').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-sources')
      .click()
      .getByText(CONNECTOR_TYPES.jdbcSource)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .getByTestId('new-connector-dialog')
      .within(() => {
        cy.getByText('ADD').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-topics')
      .click()
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .getByText('ADD')
      .click()
      .wait('@putPipeline');

    cy.wait(1000); // Temp workaround, need this to make sure Cypress can get the right element to work on

    cy.getByText('HDFSSink')
      .click({ force: true })
      .wait('@getConnector')
      .getByText('core')
      .click()
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 1);

    cy.wait(1000); // Temp workaround, need this to make sure Cypress can get the right element to work on

    cy.getByText('JDBCSourceConnector')
      .click({ force: true })
      .wait('@getConnector')
      .getByText('core')
      .click()
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 2);
  });

  it('connects perf source -> Topic', () => {
    const perfName = generate.serviceName({ prefix: 'connector' });
    const topicName = Cypress.env('TOPIC_NAME');

    cy.getByTestId('toolbar-sources')
      .click()
      .getByText('Add a new source connector')
      .should('be.exist')
      .getByText(CONNECTOR_TYPES.perfSource)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('myconnector')
      .type(perfName)
      .getByTestId('new-connector-dialog')
      .within(() => {
        cy.getByText('ADD').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-topics')
      .click()
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .getByText('ADD')
      .click()
      .wait('@putPipeline')
      .getByText('PerfSource')
      .click({ force: true })
      .getByText('core')
      .click()
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${topicName}]`)
      .click()
      .wait(2000)
      .wait('@putPipeline')
      .getByTestId('start-btn')
      .click({ force: true })
      .wait('@putConnector')
      .wait('@getPipeline') // Waiting for multiple `GET pipeline` requests here, so the metrics data can be ready for us to test
      .wait('@getPipeline')
      .wait('@getPipeline')
      .wait('@getPipeline')
      .wait('@getPipeline')
      .getByText(`Metrics (${perfName})`)
      .should('have.length', 1)
      .get('@getPipeline')
      .then(xhr => {
        const perf = xhr.response.body.objects.find(
          object => object.name === perfName,
        );

        const metricsCount = perf.metrics.meters.length;
        cy.get('[data-testid="metric-item"]').should(
          'have.length',
          metricsCount,
        );
      })
      // Element is effective width and height of 0x0, even though it is not.
      // reference https://github.com/cypress-io/cypress/issues/695
      // 1.We should be able to write an assertion that checks on non-zero width  where the tests will not move forward until it is greater than non-zero
      // reference https://github.com/cypress-io/cypress/issues/695#issuecomment-379817133
      // 2.Topic click cause refresh cypress couldn't get DOM event so click unable to end, we need click two time.
      // reference https://github.com/cypress-io/cypress/issues/695#issuecomment-493578023
      .get('@getPipeline')
      .getByTestId(`topic-${topicName}`)
      .invoke('width')
      .should('be.gt', 0)
      .getByTestId(`topic-${topicName}`)
      .click({ force: true })
      .getByTestId(`topic-${topicName}`)
      .click({ force: true })

      .getByText(`Metrics (${topicName})`)
      .should('have.length', 1)
      .getByTestId('expandIcon')
      .click({ force: true })
      .get('@getPipeline')
      .then(xhr => {
        const topic = xhr.response.body.objects.find(
          object => object.name === topicName,
        );

        const metricsCount = topic.metrics.meters.length;
        cy.get('[data-testid="metric-item"]').should(
          'have.length',
          metricsCount,
        );
      });
  });

  it('should display an error icon with the correct URL links to logs page when fail to start a connector', () => {
    cy.getByTestId('toolbar-sources')
      .click()
      .getByText(CONNECTOR_TYPES.ftpSource)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .getByTestId('new-connector-dialog')
      .within(() => {
        cy.getByText('ADD').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-topics')
      .click()
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .getByText('ADD')
      .click()
      .wait('@putPipeline');
    cy.getByText('FtpSource')
      .click({ force: true })
      .wait('@getConnector')
      .getByText('core')
      .click({ force: true })
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .getByText('common')
      .click({ force: true })
      .getByTestId('input_folder')
      .type(generate.randomString())
      .getByTestId('completed_folder')
      .type(generate.randomString())
      .getByTestId('error_folder')
      .type(generate.randomString())
      .getByTestId('ftp_hostname')
      .type(generate.ip())
      .getByTestId('ftp_port')
      .type(generate.port())
      .getByTestId('ftp_user_name')
      .type(generate.userName())
      .getByTestId('ftp_user_password')
      .type(generate.password())
      .wait(3000) // UI has one sec throttle, so we need to wait a bit time
      .getByText('Test your configs')
      .click()
      .wait('@putValidateConnector')
      .getByTestId('start-btn')
      .click()
      .wait('@putConnector')
      .get('@getPipeline')
      .then(xhr => {
        const ftpSource = xhr.response.body.objects.find(
          object => object.className === CONNECTOR_TYPES.ftpSource,
        );
        const connectorState = ftpSource.state;
        expect(connectorState).to.equal('FAILED');
      })
      .getByTestId('status-icon')
      .parent()
      .should('have.attr', 'href')
      .then(href => {
        cy.visit(href);
      });

    cy.url().should('include', `/logs/workers/${Cypress.env('WORKER_NAME')}`);
    cy.get('h2').should(
      'have.text',
      `Error log of cluster ${Cypress.env('WORKER_NAME')}`,
    );
  });
});
