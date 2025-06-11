import axios from 'axios';

const API_URL = 'http://localhost:8100/api';

export const getAllArtists = () => axios.get(`${API_URL}/artist/all`);
export const getArtistById = (id) => axios.get(`${API_URL}/artist/${id}`);
export const getArtistsByArtTitle = (title) => axios.get(`${API_URL}/artist/by-art`, { params: { artTitle: title } });
export const searchArtists = (firstName, lastName) => axios.get(`${API_URL}/artist/name`, { params: { firstName, lastName } });
export const createArtist = (artist) => axios.post(`${API_URL}/artist/add`, artist);
export const updateArtist = (id, artist) => axios.put(`${API_URL}/artist/${id}`, artist);
export const patchArtist = (id, artist) => axios.patch(`${API_URL}/artist/${id}`, artist);
export const deleteArtist = (id) => axios.delete(`${API_URL}/artist/${id}`);
export const addBulkArtists = (artists) => axios.post(`${API_URL}/artist/bulk`, artists);

export const getAllArts = () => axios.get(`${API_URL}/art/all`);
export const getArtById = (id) => axios.get(`${API_URL}/art/${id}`);
export const getArtByTitle = (title) => axios.get(`${API_URL}/art/title`, { params: { title } });
export const getArtsByArtistName = (artistName) => axios.get(`${API_URL}/art/by-artist`, { params: { artistName } });
export const getArtsByClassificationId = (id) => axios.get(`${API_URL}/art/by-classificationid`, { params: { id } });
export const getArtsByClassificationName = (name) => axios.get(`${API_URL}/art/by-classification`, { params: { name } });
export const createArt = (art) => axios.post(`${API_URL}/art/add`, art);
export const updateArt = (id, art) => axios.put(`${API_URL}/art/${id}`, art);
export const patchArt = (id, art) => axios.patch(`${API_URL}/art/${id}`, art);
export const deleteArt = (id) => axios.delete(`${API_URL}/art/${id}`);
export const addBulkArts = (arts) => axios.post(`${API_URL}/art/bulk`, arts);

export const getAllClassifications = () => axios.get(`${API_URL}/classification/all`);
export const getClassificationById = (id) => axios.get(`${API_URL}/classification/${id}`);
export const getClassificationsByArtTitle = (artTitle) => axios.get(`${API_URL}/classification/by-art`, { params: { artTitle } });
export const getClassificationsByName = (name) => axios.get(`${API_URL}/classification/name`, { params: { name } });
export const createClassification = (classification) => axios.post(`${API_URL}/classification/add`, classification);
export const updateClassification = (id, classification) => axios.put(`${API_URL}/classification/${id}`, classification);
export const patchClassification = (id, classification) => axios.patch(`${API_URL}/classification/${id}`, classification);
export const deleteClassification = (id) => axios.delete(`${API_URL}/classification/${id}`);
export const addBulkClassifications = (classifications) => axios.post(`${API_URL}/classification/bulk`, classifications);