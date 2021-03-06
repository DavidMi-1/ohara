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
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogTitle from '@material-ui/core/DialogTitle';
import Button from '@material-ui/core/Button';
import CircularProgress from '@material-ui/core/CircularProgress';

import DrabblePaper from './DrabblePaper';

const MuiDialog = props => {
  const {
    open,
    handelClose,
    title,
    confirmBtnText = 'Add',
    cancelBtnText = 'Cancel',
    handleConfirm,
    children,
    confirmDisabled = false,
    maxWidth = 'xs',
    loading,
    testId,
  } = props;
  return (
    <Dialog
      open={open}
      onClose={handelClose}
      maxWidth={maxWidth}
      PaperComponent={DrabblePaper}
      fullWidth
    >
      <div data-testid={testId}>
        <DialogTitle>{title}</DialogTitle>
        {children}
        <DialogActions>
          <Button onClick={handelClose} color="primary">
            {cancelBtnText}
          </Button>

          <Button
            onClick={handleConfirm}
            color="primary"
            autoFocus
            disabled={confirmDisabled}
          >
            {!loading && confirmBtnText}
            {loading && (
              <CircularProgress data-testid="dialog-loader" size={24} />
            )}
          </Button>
        </DialogActions>
      </div>
    </Dialog>
  );
};

MuiDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  handelClose: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  confirmBtnText: PropTypes.string,
  cancelBtnText: PropTypes.string,
  maxWidth: PropTypes.string,
  handleConfirm: PropTypes.func.isRequired,
  children: PropTypes.any.isRequired,
  confirmDisabled: PropTypes.bool,
  loading: PropTypes.bool,
  testId: PropTypes.string,
};

export default MuiDialog;
