import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { TextField, Button, Box, Snackbar } from '@mui/material';
import { createArtist, updateArtist, getArtistById } from '../api';

function ArtistForm() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [artist, setArtist] = useState({ firstName: '', middleName: '', lastName: '' });
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    useEffect(() => {
        if (id) {
            fetchArtist();
        }
    }, [id]);

    const fetchArtist = async () => {
        try {
            const response = await getArtistById(id);
            setArtist(response.data);
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при загрузке артиста';
            setSnackbarMessage(errorMessage);
            setSnackbarOpen(true);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const artistData = { firstName: artist.firstName, middleName: artist.middleName, lastName: artist.lastName };
            if (id) {
                await updateArtist(id, artistData);
                setSnackbarMessage('Артист успешно обновлен');
            } else {
                await createArtist(artistData);
                setSnackbarMessage('Артист успешно добавлен');
            }
            setSnackbarOpen(true);
            navigate('/artists');
            window.location.reload();
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при сохранении артиста';
            setSnackbarMessage(errorMessage);
            setSnackbarOpen(true);
        }
    };

    const handleCloseSnackbar = () => {
        setSnackbarOpen(false);
    };

    return (
        <div>
            <h2>{id ? 'Редактировать артиста' : 'Добавить артиста'}</h2>
            <form onSubmit={handleSubmit}>
                <Box mb={2}>
                    <TextField
                        label="Имя"
                        value={artist.firstName || ''}
                        onChange={(e) => setArtist({ ...artist, firstName: e.target.value })}
                        fullWidth
                        required
                    />
                </Box>
                <Box mb={2}>
                    <TextField
                        label="Отчество"
                        value={artist.middleName || ''}
                        onChange={(e) => setArtist({ ...artist, middleName: e.target.value })}
                        fullWidth
                    />
                </Box>
                <Box mb={2}>
                    <TextField
                        label="Фамилия"
                        value={artist.lastName || ''}
                        onChange={(e) => setArtist({ ...artist, lastName: e.target.value })}
                        fullWidth
                        required
                    />
                </Box>
                <Button type="submit" variant="contained">Сохранить</Button>
                <Button variant="outlined" onClick={() => navigate('/artists')}>Отмена</Button>
            </form>
            <Snackbar
                open={snackbarOpen}
                autoHideDuration={6000}
                onClose={handleCloseSnackbar}
                message={snackbarMessage}
            />
        </div>
    );
}

export default ArtistForm;