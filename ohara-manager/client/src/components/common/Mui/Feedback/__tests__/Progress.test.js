import React from 'react';
import { cleanup, render, waitForElement } from '@testing-library/react';
//import {act} from 'react-dom/test-utils';
import { renderHook, act } from '@testing-library/react-hooks';             
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import Progress from '../Progress';
//import { act } from 'react-test-renderer';
//import { from } from 'rxjs';
//import { Container } from '@material-ui/core';

const setup = (override = {}) => {
    return {
        steps: [],
        open: true,
      activeStep:0,
      ...override,
    };
};
// steps: PropTypes.array.isRequired,
//   open: PropTypes.bool.isRequired,
//   activeStep: PropTypes.number.isRequired,
//   brfbrf: PropTypes.bool,
//   createTitle: PropTypes.string,
//   deleteTitle: PropTypes.string, 

describe('<Progress />', () => {
    afterEach(() => {
      cleanup();
      jest.clearAllMocks();
    });
  
    it('renders progress', async () => {
      render(<Progress {...setup()} />);
    });

    it('renders progress steps', async () => {
        const props = setup({steps: [generate.name(),generate.name()]});
        const {getByText, getAllByTestId} = await waitForElement(() => render(<Progress {...props} />));

        getByText(props.steps[0]);
        getByText(props.steps[1]);

        const step1 = getAllByTestId('testId')[0].querySelector('svg');
        const step2 = getAllByTestId('testId')[1].querySelector('svg');
        
        step1.getAttribute('class').includes('MuiStepIcon-active');
        expect(step2).toHaveAttribute('class', 'MuiSvgIcon-root MuiStepIcon-root');
        
        getByText('1');
        getByText('2');
    });

    it('renders progress active step 2/3', async () => {
        const props = setup({steps: [generate.name(),generate.name(),generate.name()], activeStep:1});
        const {getByText,getAllByTestId,queryByText,debug} = await waitForElement(() => render(<Progress {...props} />));
        
        const step1 = getAllByTestId('testId')[0].querySelector('svg');
        const step2 = getAllByTestId('testId')[1].querySelector('svg');
        const step3 = getAllByTestId('testId')[2].querySelector('svg');
        
        step1.getAttribute('class').includes('MuiStepIcon-completed');
        step2.getAttribute('class').includes('MuiStepIcon-active');
        expect(step3).toHaveAttribute('class', 'MuiSvgIcon-root MuiStepIcon-root');
        
        expect(queryByText('1')).toBeNull();
        getByText('2');
        getByText('3');
    });

    it('renders progress active step 3/3', async () => {
        const props = setup({steps: [generate.name(),generate.name(),generate.name()], activeStep:2});
        const {getByText,getAllByTestId,queryByText,debug} = await waitForElement(() => render(<Progress {...props} />));
        
        const step1 = getAllByTestId('testId')[0].querySelector('svg');
        const step2 = getAllByTestId('testId')[1].querySelector('svg');
        const step3 = getAllByTestId('testId')[2].querySelector('svg');
        
        step1.getAttribute('class').includes('MuiStepIcon-completed');
        step2.getAttribute('class').includes('MuiStepIcon-completed');
        step3.getAttribute('class').includes('MuiStepIcon-active');
        
        expect(queryByText('1')).toBeNull();
        expect(queryByText('2')).toBeNull();
        getByText('3');
    });

    fit('renders progress delete type',  () => {
        const {result, rerender} = renderHook(({props}) => Progress(props),{
            initialProps: { 
                props: setup({steps: [generate.name(),generate.name(),generate.name()], activeStep:2}) 
            }
        });
        rerender({props: setup({deleteType:true})})
        //console.log(result.current.props.children[0]);
        console.log(result.current.props.children[0].props.children);
        console.log(result.current.props.children[1]);
        console.log(result.current.props.children[1].props.children);
        //console.log(result.current.props.children[2]);
        //console.log(result.current.props.children[3]);
        act(() => {
            result.current.props.children[0].props.children = () => {return 'Deleting'};
            
        })
        //expect(result.current.title).toBe('Deleting');

    });
});