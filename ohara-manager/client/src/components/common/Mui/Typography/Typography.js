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
import PropTypes from 'prop-types';
import Typography from '@material-ui/core/Typography';

const MuiTypography = ({
  variant,
  children,
  testId = 'typography',
  ...rest
}) => {
  return (
    <Typography {...rest} variant={variant} data-testid={testId}>
      {children}
    </Typography>
  );
};

MuiTypography.propTypes = {
  variant: PropTypes.string.isRequired,
  children: PropTypes.any.isRequired,
  testId: PropTypes.string,
};

export default MuiTypography;
