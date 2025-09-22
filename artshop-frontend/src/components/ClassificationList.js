import React, { useState, useEffect } from 'react';
import {
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    Paper, TextField, Button, Box, Typography
} from '@mui/material';
import { Link } from 'react-router-dom';
import axios from 'axios';

const ClassificationList = () => {
    const [classifications, setClassifications] = useState([]);
    const [searchName, setSearchName] = useState('');

    useEffect(() => {
        fetchClassifications();
    }, []);

    const fetchClassifications = async () => {
        try {
            const response = await axios.get('http://localhost:8100/api/classification/all');
            setClassifications(response.data);
        } catch (error) {
            console.error('Error fetching classifications:', error);
        }
    };

    const searchClassifications = async () => {
        try {
            const response = await axios.get(`http://localhost:8100/api/classification/name?name=${searchName}`);
            setClassifications(response.data);
        } catch (error) {
            console.error('Error searching classifications:', error);
        }
    };

    const deleteClassification = async (id) => {
        if (window.confirm('Вы уверены, что хотите удалить эту классификацию?')) {
            try {
                await axios.delete(`http://localhost:8100/api/classification/${id}`);
                fetchClassifications();
            } catch (error) {
                console.error('Error deleting classification:', error);
            }
        }
    };

    return (
        <div>
            <Typography variant="h4" gutterBottom>
                Список классификаций
            </Typography>

            <Box sx={{
                mb: 3,
                display: 'flex',
                gap: 2,
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%'
            }}>
                <TextField
                    label="Классификация"
                    value={searchName}
                    onChange={(e) => setSearchName(e.target.value)}
                    size="small"
                />
                <Button variant="contained" onClick={searchClassifications}>
                    поиск
                </Button>
            </Box>

            <Box sx={{ display: 'flex', justifyContent: 'center', mb: 3 }}>
                <Button
                    variant="contained"
                    color="primary"
                    component={Link}
                    to="/classifications/add"
                >
                    ДОБАВИТЬ КЛАССИФИКАЦИЮ
                </Button>
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>№</TableCell>
                            <TableCell>Название</TableCell>
                            <TableCell>Описание</TableCell>
                            <TableCell>Произведения</TableCell>
                            <TableCell>Действия</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {classifications.map((classification, index) => (
                            <TableRow key={classification.id}>
                                <TableCell>{index + 1}</TableCell>
                                <TableCell>{classification.name}</TableCell>
                                <TableCell>{classification.description}</TableCell>
                                <TableCell>
                                    {classification.artworkCount > 0 ? (
                                        classification.artworkTitles.join(', ')
                                    ) : (
                                        'Нет произведений'
                                    )}
                                </TableCell>
                                <TableCell>
                                    <Button
                                        component={Link}
                                        to={`/classifications/edit/${classification.id}`}
                                        variant="outlined"
                                        size="small"
                                        sx={{ mr: 1 }}
                                    >
                                        РЕДАКТИРОВАТЬ
                                    </Button>
                                    <Button
                                        variant="outlined"
                                        color="error"
                                        size="small"
                                        onClick={() => deleteClassification(classification.id)}
                                    >
                                        УДАЛИТЬ
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    );
};

export default ClassificationList;