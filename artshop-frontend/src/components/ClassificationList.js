import React, { useState, useEffect } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, TextField, Box, Snackbar, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { Link } from 'react-router-dom';
import { getAllClassifications, getClassificationsByName, deleteClassification } from '../api';

function ClassificationList() {
    const [classifications, setClassifications] = useState([]);
    const [name, setName] = useState('');
    const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
    const [deleteId, setDeleteId] = useState(null);
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    useEffect(() => {
        fetchClassifications();
    }, []);

    const fetchClassifications = async () => {
        try {
            const response = await getAllClassifications();
            setClassifications(response.data);
        } catch (error) {
            setSnackbarMessage('Ошибка при загрузке классификаций: ' + error.message);
            setSnackbarOpen(true);
        }
    };

    const handleSearchByName = async () => {
        try {
            const response = await getClassificationsByName(name);
            setClassifications(response.data);
        } catch (error) {
            setSnackbarMessage('Ошибка при поиске по имени: ' + error.message);
            setSnackbarOpen(true);
        }
    };

    const handleDelete = async (id) => {
        setDeleteId(id);
        setOpenDeleteDialog(true);
    };

    const confirmDelete = async () => {
        setOpenDeleteDialog(false);
        try {
            await deleteClassification(deleteId);
            setClassifications(classifications.filter(classification => classification.id !== deleteId));
            setSnackbarMessage('Классификация успешно удалена');
            setSnackbarOpen(true);
        } catch (error) {
            setSnackbarMessage('Ошибка при удалении классификации: ' + error.message);
            setSnackbarOpen(true);
        }
    };

    const handleCloseSnackbar = () => {
        setSnackbarOpen(false);
    };

    const handleCloseDeleteDialog = () => {
        setOpenDeleteDialog(false);
        setDeleteId(null);
    };

    return (
        <div>
            <h2>Список классификаций</h2>
            <Box mb={2}>
                <TextField
                    label="Имя классификации"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    style={{ marginRight: '10px' }}
                />
                <Button variant="contained" onClick={handleSearchByName}>Поиск</Button>
            </Box>
            <Button variant="contained" component={Link} to="/classifications/add">Добавить классификацию</Button>
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
                                <TableCell>{classification.arts?.map(art => art.title).join(', ') || 'Нет произведений'}</TableCell>
                                <TableCell>
                                    <Button component={Link} to={`/classifications/edit/${classification.id}`}>Редактировать</Button>
                                    <Button color="error" onClick={() => handleDelete(classification.id)}>Удалить</Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            <Snackbar
                open={snackbarOpen}
                autoHideDuration={6000}
                onClose={handleCloseSnackbar}
                message={snackbarMessage}
            />
            <Dialog
                open={openDeleteDialog}
                onClose={handleCloseDeleteDialog}
            >
                <DialogTitle>Подтверждение удаления</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Вы уверены, что хотите удалить эту классификацию? Это действие нельзя отменить.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseDeleteDialog} color="primary">Отмена</Button>
                    <Button onClick={confirmDelete} color="error">Удалить</Button>
                </DialogActions>
            </Dialog>
        </div>
    );
}

export default ClassificationList;