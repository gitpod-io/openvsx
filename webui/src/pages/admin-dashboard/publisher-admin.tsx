/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useContext, createContext } from 'react';
import { Typography, Box } from '@material-ui/core';

import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { PublisherInfo } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { StyledInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';
import { PublisherDetails } from './publisher-details';

export const UpdateContext = createContext({ handleUpdate: () => { }, setLoading: (value: React.SetStateAction<boolean>) => { } });
export const PublisherAdmin: FunctionComponent = props => {
    const [loading, setLoading] = useState(false);

    const { pageSettings, service, user, handleError } = useContext(MainContext);

    const [inputValue, setInputValue] = useState('');
    const onChangeInput = (name: string) => {
        setInputValue(name);
    };

    const [publisher, setPublisher] = useState<PublisherInfo | undefined>();
    const [notFound, setNotFound] = useState('');
    const fetchPublisher = async () => {
        const publisherName = inputValue;
        try {
            setLoading(true);
            if (publisherName !== '') {
                const publisher = await service.admin.getPublisherInfo('github', publisherName);
                setNotFound('');
                setPublisher(publisher);
            } else {
                setNotFound('');
                setPublisher(undefined);
            }
            setLoading(false);
        } catch (err) {
            if (err && err.status && err.status === 404) {
                setNotFound(publisherName);
                setPublisher(undefined);
            } else {
                handleError(err);
            }
            setLoading(false);
        }
    };

    const handleUpdate = () => {
        fetchPublisher();
    };

    return (<>
        <DelayedLoadIndicator loading={loading} />
        <SearchListContainer
            searchContainer={
                [<StyledInput placeholder='Publisher Name' key='pi' onSubmit={fetchPublisher} onChange={onChangeInput} />]
            }
            listContainer={
                publisher && pageSettings && user ?
                    <UpdateContext.Provider value={{ handleUpdate, setLoading }}>
                        <PublisherDetails publisherInfo={publisher} />
                    </UpdateContext.Provider>
                    : notFound ?
                        <Box display='flex' flexDirection='column'>
                            <Typography variant='body1' color='error'>
                                Publisher {notFound} not found.
                            </Typography>
                        </Box>
                        : ''
            }
        />
    </>);
};