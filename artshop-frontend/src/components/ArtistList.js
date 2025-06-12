import React, { useState, useEffect } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, TextField, Box, Snackbar, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { Link } from 'react-router-dom';
import { getAllArtists, searchArtists, deleteArtist } from '../api';

function ArtistList() {
    const [artists, setArtists] = useState([]);
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
    const [deleteId, setDeleteId] = useState(null);
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    useEffect(() => {
        fetchArtists();
    }, []);

    const fetchArtists = async () => {
        try {
            const response = await getAllArtists();
            setArtists(response.data);
        } catch (error) {
            setSnackbarMessage('Ошибка при загрузке артистов: ' + error.message);
            setSnackbarOpen(true);
        }
    };

    const handleSearchByName = async () => {
        try {
            const response = await searchArtists(firstName, lastName);
            setArtists(response.data);
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
            await deleteArtist(deleteId);
            setArtists(artists.filter(artist => artist.id !== deleteId));
            setSnackbarMessage('Артист успешно удален');
            setSnackbarOpen(true);
        } catch (error) {
            setSnackbarMessage('Ошибка при удалении артиста: ' + error.message);
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
            <h2>Список артистов</h2>
            <Box mb={2}>
                <TextField
                    label="Имя"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    style={{ marginRight: '10px' }}
                />
                <TextField
                    label="Фамилия"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    style={{ marginRight: '10px' }}
                />
                <Button variant="contained" onClick={handleSearchByName}>Поиск</Button>
            </Box>
            <Button variant="contained" component={Link} to="/artists/add">Добавить артиста</Button>
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>№</TableCell>
                            <TableCell>Имя</TableCell>
                            <TableCell>Отчество</TableCell>
                            <TableCell>Фамилия</TableCell>
                            <TableCell>Произведения</TableCell>
                            <TableCell>Действия</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {artists.map((artist, index) => (
                            <TableRow key={artist.id}>
                                <TableCell>{index + 1}</TableCell>
                                <TableCell>{artist.firstName}</TableCell>
                                <TableCell>{artist.middleName}</TableCell>
                                <TableCell>{artist.lastName}</TableCell>
                                <TableCell>{artist.arts?.map(art => art.title).join(', ') || 'Нет произведений'}</TableCell>
                                <TableCell>
                                    <Button component={Link} to={`/artists/edit/${artist.id}`}>Редактировать</Button>
                                    <Button color="error" onClick={() => handleDelete(artist.id)}>Удалить</Button>
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
                        Вы уверены, что хотите удалить этого артиста? Это действие нельзя отменить.
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

export default ArtistList;