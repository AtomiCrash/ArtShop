import React, { useState, useEffect } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, TextField, Box, Snackbar, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { Link } from 'react-router-dom';
import { getAllArts, getArtByTitle, getArtsByClassificationName, deleteArt } from '../api';

function ArtList() {
    const [arts, setArts] = useState([]);
    const [title, setTitle] = useState('');
    const [classificationName, setClassificationName] = useState('');
    const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
    const [deleteId, setDeleteId] = useState(null);
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    useEffect(() => {
        fetchArts();
    }, []);

    const fetchArts = async () => {
        try {
            const response = await getAllArts();
            setArts(response.data);
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при загрузке произведений';
            setSnackbarMessage(errorMessage);
            setSnackbarOpen(true);
        }
    };

    const handleSearchByTitle = async () => {
        try {
            const response = await getArtByTitle(title);
            setArts(response.data ? [response.data] : []);
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при поиске по названию';
            setSnackbarMessage(errorMessage);
            setSnackbarOpen(true);
        }
    };

    const handleSearchByClassificationName = async () => {
        try {
            const response = await getArtsByClassificationName(classificationName);
            setArts(response.data);
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при поиске по имени классификации';
            setSnackbarMessage(errorMessage);
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
            await deleteArt(deleteId);
            setArts(arts.filter(art => art.id !== deleteId));
            setSnackbarMessage('Произведение успешно удалено');
            setSnackbarOpen(true);
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при удалении произведения';
            setSnackbarMessage(errorMessage);
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
            <h2>Список произведений</h2>
            <Box mb={2}>
                <TextField
                    label="Поиск по названию"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    style={{ marginRight: '10px' }}
                />
                <Button variant="contained" onClick={handleSearchByTitle}>Поиск</Button>
            </Box>
            <Box mb={2}>
                <TextField
                    label="Классификация"
                    value={classificationName}
                    onChange={(e) => setClassificationName(e.target.value)}
                    style={{ marginRight: '10px' }}
                />
                <Button variant="contained" onClick={handleSearchByClassificationName}>Поиск</Button>
            </Box>
            <Button variant="contained" component={Link} to="/arts/add">Добавить произведение</Button>
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>№</TableCell>
                            <TableCell>Название</TableCell>
                            <TableCell>Год</TableCell>
                            <TableCell>Классификация</TableCell>
                            <TableCell>Артисты</TableCell>
                            <TableCell>Действия</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {arts.map((art, index) => (
                            <TableRow key={art.id}>
                                <TableCell>{index + 1}</TableCell>
                                <TableCell>{art.title}</TableCell>
                                <TableCell>{art.year}</TableCell>
                                <TableCell>{art.classification?.name || 'Без классификации'}</TableCell>
                                <TableCell>{art.artists?.map(artist => `${artist.firstName} ${artist.lastName}`).join(', ') || 'Без артистов'}</TableCell>
                                <TableCell>
                                    <Button component={Link} to={`/arts/edit/${art.id}`}>Редактировать</Button>
                                    <Button color="error" onClick={() => handleDelete(art.id)}>Удалить</Button>
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
                        Вы уверены, что хотите удалить это произведение? Это действие нельзя отменить.
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

export default ArtList;