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

import React from 'react';
import { cleanup, waitForElement, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import { divide, floor } from 'lodash';

import * as generate from 'utils/generate';
import Overview from '../Overview';
import { renderWithProvider } from 'utils/testUtils';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';

jest.mock('api/infoApi');
jest.mock('components/controller');
afterEach(cleanup);

// Skip the tests for now. We should mock the XHR requests in the test
describe('<Overview />', () => {
  const brokerClusterName = generate.serviceName();
  const topics = generate.topics({ brokerClusterName });
  const broker = {
    clientPort: 39517,
    deadNodes: [],
    exporterPort: 35700,
    imageName: "oharastream/broker:0.7.0",
    jmxPort: 57062,
    lastModified: 1566888361731,
    name: "4d6xkmy02z",
    nodeNames: ["ohara-demo-01", "ohara-demo-02", "ohara-demo-03"],
    0: "ohara-demo-01",
    1: "ohara-demo-02",
    2: "ohara-demo-03",
    state: "RUNNING",
    tags: {},
    topicSettingDefinitions: [],
    zookeeperClusterName: "kjewn1ep82",
  }

  const zookeeper = {
    clientPort: 13553,
    deadNodes: [],
    electionPort: 60473,
    imageName: "oharastream/zookeeper:0.7.0",
    lastModified: 1566888361733,
    name: "kjewn1ep82",
    nodeNames: ["ohara-demo-01"],
    0: "ohara-demo-01",
    peerPort: 34937,
    state: "RUNNING",
    tags: {},
  }

  const jars = {
    group: "wk",
    lastModified: 1566898233000,
    name: "ohara-streamapp.jar",
    size: 1937,
    tags: {},
    url: "http://ohara-demo-00:12345/v0/downloadFiles/wk/ohara-streamapp.jar",
  }

  const connectors = [{
    className: "com.island.ohara.connector.console.ConsoleSink",
    definitions: [{
      defaultValue: "sink",
      displayName: "kind",
      documentation: "kind of connector",
      editable: false,
      group: "core",
      internal: false,
      key: "kind",
      orderInGroup: 13,
      reference: "NONE",
      required: false,
      tableKeys: [],
      valueType: "STRING",
    },{
      defaultValue: "unknown",
      displayName: "version",
      documentation: "version of connector",
      editable: false,
      group: "core",
      internal: false,
      key: "version",
      orderInGroup: 10,
      reference: "NONE",
      required: false,
      tableKeys: [],
      valueType: "STRING",
    },{
      defaultValue: "unknown",
      displayName: "author",
      documentation: "author of connector",
      editable: false,
      group: "core",
      internal: false,
      key: "author",
      orderInGroup: 12,
      reference: "NONE",
      required: false,
      tableKeys: [],
      valueType: "STRING",
    }],
  }];

  const props = {
    history: {
      push: jest.fn(),
    },
    worker: {
      name: generate.name(),
      clientPort: generate.port(),
      jmxPort: generate.port(),
      connectors,
      nodeNames: ["ohara-demo-01", "ohara-demo-02", "ohara-demo-03"],
      brokerClusterName,
      imageName: generate.name(),
      tags: {
        broker: {
          name: generate.name(),
          imageName: generate.name(),
        },
        zookeeper: {
          name: generate.name(),
          imageName: generate.name(),
        },
      },
    },
  };

  jest.spyOn(useApi, 'useFetchApi').mockImplementation(url => {
    if(url === URL.TOPIC_URL ){
      return {
        data: {
          data: {
            result: topics,
          },
        },
        isLoading: false,
      };
    }
    if (url.includes(URL.FILE_URL)) {
      return {
        data: {
          data: {
            result: [jars],
          },
        },
        isLoading: false,
        refetch: jest.fn(),
      };
    }
    if(url.includes(URL.WORKER_URL)){
      return {
        data: {
          data: {
            result: [workers],
          },
        },
        isLoading: false,
        refetch: jest.fn(),
      };
    }
    if(url.includes(URL.BROKER_URL)){
      return {
        data: {
          data: {
            result: broker,
          },
        },
        isLoading: false,
      };
    }
    if(url.includes(URL.ZOOKEEPER_URL)){
      return {
        data: {
          data: {
            result: zookeeper,
          },
        },
        isLoading: false,
      };
    }
  });

  it('renders the page', async () => {
    await waitForElement(() => renderWithProvider(<Overview {...props} />));
  });

  it('renders the correct docker image name', async () => {
    const { getByText } = await renderWithProvider(<Overview {...props} />);

    getByText(`Worker Image: ${imageName}`);
  });

  // The rest of the tests are covered in the end-to-end tests
  // since these tests require a LOT of mocking, it's probably to
  // test them in the end-to-end for now

  it('renders the correct paper titles', async () => {
    const { getByText } = await renderWithProvider(<Overview {...props} />);

    getByText(`Basic info`);
    getByText(`Nodes`);
    getByText(`Topics`);
    getByText(`Connectors`);
    getByText(`Stream Jars`);
  });

  it('renders the correct basic info content', async () => {
    const { getByText } = await renderWithProvider(<Overview {...props} />);

    getByText('Worker Image: ' + imageName);
    getByText('Broker Image: ' + broker.imageName);
    getByText('Zookeeper Image: ' + zookeeper.imageName);
  });

  it('renders the correct nodes headers', async () => {
    const { getByText, getAllByText } = await renderWithProvider(<Overview {...props} />);

    getByText('Cluster type');
    getByText('Node');
    getAllByText('More info');
  });

  fit('renders the correct nodes content', async () => {
    const { getByText, getAllByText, getByTestId, getAllByTestId, getByAltText,debug } = await renderWithProvider(<Overview {...props} />);

    getByText('Worker');
    getAllByText(props.worker.nodeNames[0] + ':' + props.worker.clientPort);

    const clusterType = getAllByTestId('node-type');
    const moreInfoTestId = clusterType[0].textContent + '-' + props.worker.nodeNames[0] + ':' + props.worker.clientPort + '-icon';
    const moreInfoObject = await waitForElement(() => getByTestId(moreInfoTestId));
    fireEvent.click(moreInfoObject);
    fireEvent.mouseMove(moreInfoObject);
    debug();
    //console.log(props.worker.jmxPort);
    //getByText('Jmxport: ' + props.worker.jmxPort);
    //const tooltip = getByRole('tooltip');
    //console.log(tooltip);
  });

  it('renders the correct topics headers', async () => {
    const { getByText, getAllByText } = await renderWithProvider(<Overview {...props} />);

    getAllByText('Name');
    getByText('Partitions');
    getByText('Replication factor');
  });

  it('renders the correct topics content', async () => {
    const { getByText } = await renderWithProvider(<Overview {...props} />);

    getByText(topics[0].name);
    const partitionValue = topics[0].numberOfPartitions;
    getByText(partitionValue.toString());
    const replicaValue = topics[0].numberOfReplications;
    getByText(replicaValue.toString());
  });

  it('renders the correct connectors headers', async () => {
    const { getAllByText } = await renderWithProvider(<Overview {...props} />);

    getAllByText('Name');
    getAllByText('More info');
  });

  it('renders the correct connectors content', async () => {
    const { getByText } = await renderWithProvider(<Overview {...props} />);

    getByText('ConsoleSink');    
  });

  it('renders the correct stream jars headers', async () => {
    const { getAllByText } = await renderWithProvider(<Overview {...props} />);

    getAllByText('Jar name');
    getAllByText('File size(KB)');
  });

  it('renders the correct stream jars content', async () => {
    const { getByText } = await renderWithProvider(<Overview {...props} />);

    getByText(jars.name);
    const fileSize = floor(divide(jars.size, 1024), 1);
    getByText(fileSize.toString());
    console.log(fileSize);
  });
});
