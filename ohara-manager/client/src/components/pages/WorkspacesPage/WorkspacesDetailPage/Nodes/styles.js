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

import styled from 'styled-components';
import Button from 'components/common/Mui/Form/Button';
import { Table as MuiTable } from 'components/common/Mui/Table';

export const PageHeader = styled.div`
  margin-top: 30px;
  margin-right: 30px;
  display: flex;
`;

export const PageBody = styled.div`
  padding-top: 30px;
  padding-bottom: 35px;
  max-width: 1200px;
  width: calc(100% - 60px);
  margin: auto;
`;

export const StyledButton = styled(Button)`
  margin-left: auto;
  margin-bottom: auto;
  align-self: center;
`;

export const StyledTable = styled(MuiTable)`
  width: 80%;
`;
